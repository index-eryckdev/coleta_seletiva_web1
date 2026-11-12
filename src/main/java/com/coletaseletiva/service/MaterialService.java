package com.coletaseletiva.service;
import java.util.List;

import com.coletaseletiva.dto.CadastroDtos.*;
import com.coletaseletiva.dto.PaginaResponse;
import com.coletaseletiva.entity.*;
import com.coletaseletiva.exception.*;
import com.coletaseletiva.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaterialService {
    private final MaterialRepository materiais;
    private final MaterialAceitoRepository vinculos;

    public MaterialService(MaterialRepository materiais, MaterialAceitoRepository vinculos) {
        this.materiais = materiais;
        this.vinculos = vinculos;
    }

    public PaginaResponse<MaterialSaida> listar(String nome, Boolean ativo, Pageable pagina) {
        Specification<Material> filtro = Specification.allOf(List.of());
        if (nome != null && !nome.isBlank()) {
            filtro = filtro.and((m, q, cb) -> cb.like(cb.lower(m.get("nome")), CadastroSaidas.trecho(nome), '\\'));
        }
        if (ativo != null) {
            filtro = filtro.and((m, q, cb) -> cb.equal(m.get("ativo"), ativo));
        }
        return PaginaResponse.de(materiais.findAll(filtro, pagina).map(CadastroSaidas::material));
    }

    public MaterialSaida buscar(long id, boolean somenteAtivo) {
        Material material = materiais.findById(id)
                .filter(m -> !somenteAtivo || m.getAtivo()).orElseThrow(MaterialService::ausente);
        return CadastroSaidas.material(material);
    }

    @Transactional
    public MaterialSaida criar(ContextoAtor ator, MaterialEntrada dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        String nome = dados.nome().strip();
        if (materiais.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe um material com este nome.");
        }
        Material material = new Material(nome, CadastroSaidas.opcional(dados.descricao()),
                dados.unidadeMedida(), CadastroSaidas.opcional(dados.orientacaoPreparo()));
        return CadastroSaidas.material(materiais.save(material));
    }

    @Transactional
    public MaterialSaida atualizar(ContextoAtor ator, long id, MaterialEntrada dados) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        Material material = materiais.bloquear(id).orElseThrow(MaterialService::ausente);
        String nome = dados.nome().strip();
        if (materiais.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Já existe um material com este nome.");
        }
        if (material.getUnidadeMedida() != dados.unidadeMedida()
                && vinculos.existsByMaterialIdAndLimitePorSolicitacaoIsNotNull(id)) {
            throw new RegraNegocioException("Remova os limites dos vínculos antes de mudar a unidade do material.");
        }
        material.atualizar(nome, CadastroSaidas.opcional(dados.descricao()), dados.unidadeMedida(),
                CadastroSaidas.opcional(dados.orientacaoPreparo()));
        return CadastroSaidas.material(material);
    }

    @Transactional
    public MaterialSaida alterarAtivo(ContextoAtor ator, long id, boolean ativo) {
        ator.exigir(TipoAtor.ADMINISTRADOR);
        Material material = materiais.bloquear(id).orElseThrow(MaterialService::ausente);
        material.definirAtivo(ativo);
        return CadastroSaidas.material(material);
    }

    static RecursoNaoEncontradoException ausente() {
        return new RecursoNaoEncontradoException("Material não encontrado.");
    }
}
