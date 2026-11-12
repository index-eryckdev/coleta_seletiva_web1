package com.coletaseletiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${TEST_DB_NAME:coleta_teste}"
})
@AutoConfigureMockMvc
class ColetaApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void consultaPublicaFiltraMaterialEProtegeDadosPessoais() throws Exception {
        mvc.perform(get("/api/pontos").param("municipio", "ARACAJU").param("materialId", "3").param("size", "200"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(100))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[0].materiaisAceitos").isArray())
                .andExpect(jsonPath("$.conteudo[0].email").doesNotExist());
        mvc.perform(get("/api/pontos").param("nome", "%")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/pontos").param("sort", "inexistente")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/moradores/1")).andExpect(status().isForbidden());
        mvc.perform(get("/api/moradores/1/enderecos").headers(morador(2))).andExpect(status().isForbidden());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/solicitacoes'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/solicitacoes'].post.parameters[0].name").value("X-Ator-Tipo"));
    }

    @Test
    void cadastrosValidamUnicidadeEEndereco() throws Exception {
        String email = "morador-" + UUID.randomUUID() + "@exemplo.com";
        var pessoa = obter(enviar(post("/api/moradores"), """
                {"nome":"Morador teste", "email":"  %s  "}
                """.formatted(email.toUpperCase(java.util.Locale.ROOT))), 201);
        assertThat(pessoa.get("email").asText()).isEqualTo(email);
        enviar(post("/api/moradores"), """
                {"nome":"Duplicado", "email":"%s"}
                """.formatted(email)).andExpect(status().isConflict());
        long id = pessoa.get("id").asLong();
        enviar(post("/api/moradores/" + id + "/enderecos").headers(morador(id)), """
                {"logradouro":"Rua A","semNumero":false,"bairro":"Centro","municipio":"Lagarto","uf":"SE","cep":"49400-000"}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[0].campo").value("numero"));
        enviar(post("/api/moradores/" + id + "/enderecos").headers(morador(id)), """
                {"logradouro":"Rua A","semNumero":true,"bairro":"Centro","municipio":"Lagarto","uf":"se","cep":"49400-000"}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.cep").value("49400000"))
                .andExpect(jsonPath("$.semNumero").value(true));
        enviar(post("/api/materiais").headers(admin()), """
                {"nome":"  PAPEL  ","unidadeMedida":"KG"}
                """).andExpect(status().isConflict());
        enviar(post("/api/pontos/1/materiais").headers(admin()), """
                {"materialId":2,"limitePorSolicitacao":0}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void criaComItensEHistoricoEValidaCompatibilidade() throws Exception {
        long antes = contar("solicitacao");
        criar(1, 3, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":1},{"materialId":2,"quantidadeEstimada":1}]
                """).andExpect(status().isConflict());
        criar(1, 1, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":51}]
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[0].campo").value("itens[0].quantidadeEstimada"));
        criar(1, 1, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":0}]
                """).andExpect(status().isBadRequest());
        criar(1, 1, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":2},{"materialId":1,"quantidadeEstimada":1}]
                """).andExpect(status().isBadRequest());
        criar(2, 3, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":2}]
                """).andExpect(status().isNotFound());
        criar(1, 2, enderecoDemo(false), """
                [{"materialId":3,"quantidadeEstimada":2}]
                """).andExpect(status().isConflict());
        assertThat(contar("solicitacao")).isEqualTo(antes);

        var resposta = obter(criar(1, 1, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":12.5},{"materialId":2,"quantidadeEstimada":8}]
                """), 201);
        long id = id(resposta);
        assertThat(resposta.get("itens").size()).isEqualTo(2);
        assertThat(resposta.at("/historico/0/situacaoNova").asText()).isEqualTo("SOLICITADA");
        assertThat(jdbc.queryForObject("select count(*) from item_solicitacao where solicitacao_id = ?", Long.class, id)).isEqualTo(2);
        mvc.perform(get("/api/solicitacoes/" + id).headers(morador(2))).andExpect(status().isForbidden());
        mvc.perform(get("/api/solicitacoes").headers(operador(2)).param("pontoId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void desfazSolicitacaoEItensQuandoHistoricoFalha() throws Exception {
        long solicitacoes = contar("solicitacao");
        long itens = contar("item_solicitacao");
        long historicos = contar("historico");
        jdbc.execute("""
                CREATE FUNCTION teste_impedir_historico() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF EXISTS (SELECT 1 FROM solicitacao WHERE id = NEW.solicitacao_id AND observacoes = 'qa-rollback') THEN
                        RAISE EXCEPTION 'falha simulada no histórico' USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        try {
            jdbc.execute("""
                    CREATE TRIGGER teste_impedir_historico BEFORE INSERT ON historico
                    FOR EACH ROW EXECUTE FUNCTION teste_impedir_historico()
                    """);
            enviar(post("/api/solicitacoes").headers(morador(1)), """
                    {"pontoId":1,"enderecoId":%d,"observacoes":"qa-rollback",
                     "itens":[{"materialId":1,"quantidadeEstimada":2},{"materialId":2,"quantidadeEstimada":3}]}
                    """.formatted(enderecoDemo(false))).andExpect(status().isConflict());
            assertThat(contar("solicitacao")).isEqualTo(solicitacoes);
            assertThat(contar("item_solicitacao")).isEqualTo(itens);
            assertThat(contar("historico")).isEqualTo(historicos);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS teste_impedir_historico ON historico");
            jdbc.execute("DROP FUNCTION teste_impedir_historico()");
        }
    }

    @Test
    void preservaEnderecoEUnidadeEProtegeVinculosAbertos() throws Exception {
        long material = obter(enviar(post("/api/materiais").headers(admin()), """
                {"nome":"Embalagem %s","unidadeMedida":"UNIDADE"}
                """.formatted(UUID.randomUUID())), 201).get("id").asLong();
        long ponto = obter(enviar(post("/api/pontos").headers(admin()), """
                {"nome":"Ponto teste","ativo":true,"recolhimentoDomiciliar":true,
                 "endereco":{"logradouro":"Rua C","semNumero":true,"bairro":"Centro","municipio":"Lagarto","uf":"SE","cep":"49400000"}}
                """), 201).get("id").asLong();
        String vinculos = "/api/pontos/" + ponto + "/materiais";
        enviar(post(vinculos).headers(admin()), """
                {"materialId":%d}
                """.formatted(material)).andExpect(status().isCreated());
        enviar(post(vinculos).headers(admin()), """
                {"materialId":%d}
                """.formatted(material)).andExpect(status().isConflict());
        criar(1, ponto, enderecoDemo(false), """
                [{"materialId":%d,"quantidadeEstimada":1.5}]
                """.formatted(material)).andExpect(status().isBadRequest());
        var criado = obter(criar(1, ponto, enderecoDemo(false), """
                [{"materialId":%d,"quantidadeEstimada":2}]
                """.formatted(material)), 201);
        var alterado = obter(enviar(put("/api/solicitacoes/" + id(criado)).headers(morador(1)), """
                {"versao":0,"enderecoId":%d,"itens":[{"materialId":%d,"quantidadeEstimada":3}]}
                """.formatted(enderecoDemo(true), material)), 200);
        assertThat(alterado.at("/solicitacao/versao").asInt()).isEqualTo(1);
        assertThat(alterado.at("/endereco/semNumero").asBoolean()).isTrue();
        assertThat(alterado.at("/itens/0/quantidadeEstimada").asInt()).isEqualTo(3);
        enviar(put("/api/materiais/" + material).headers(admin()), """
                {"nome":"Nome atualizado %s","unidadeMedida":"KG"}
                """.formatted(UUID.randomUUID())).andExpect(status().isOk());
        mvc.perform(patch("/api/pontos/" + ponto + "/inativar").headers(admin())).andExpect(status().isConflict());
        mvc.perform(delete(vinculos + "/" + material).headers(admin())).andExpect(status().isConflict());
        mvc.perform(get("/api/solicitacoes/" + id(criado)).headers(morador(1))).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].unidade").value("UNIDADE"))
                .andExpect(jsonPath("$.endereco.logradouro").value("Avenida Principal"));
        enviar(patch("/api/solicitacoes/" + id(criado) + "/cancelar").headers(morador(1)), """
                {"versao":1,"motivo":"Encerrar teste"}
                """).andExpect(status().isOk());
        mvc.perform(delete(vinculos + "/" + material).headers(admin())).andExpect(status().isNoContent());
        mvc.perform(patch("/api/pontos/" + ponto + "/inativar").headers(admin())).andExpect(status().isOk());
        mvc.perform(get("/api/pontos/" + ponto)).andExpect(status().isNotFound());
    }

    @Test
    void agendaReagendaConcluiEContaQuantidadesEfetivas() throws Exception {
        long id = id(obter(criar(1, 1, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":2},{"materialId":2,"quantidadeEstimada":3}]
                """), 201));
        String caminho = "/api/solicitacoes/" + id;
        String futuro = OffsetDateTime.now().plusDays(2).toString();
        enviar(patch(caminho + "/agendar").headers(operador(2)), agenda(0, futuro)).andExpect(status().isForbidden());
        enviar(patch(caminho + "/agendar").headers(operador(1)), agenda(0, "2000-01-01T10:00:00-03:00"))
                .andExpect(status().isBadRequest());
        enviar(patch(caminho + "/recusar").headers(operador(1)), """
                {"versao":0,"motivo":" "}
                """).andExpect(status().isBadRequest());
        enviar(patch(caminho + "/iniciar").headers(operador(1)), versao(0)).andExpect(status().isConflict());
        enviar(patch(caminho + "/agendar").headers(operador(1)), agenda(0, futuro)).andExpect(status().isOk());
        enviar(patch(caminho + "/iniciar").headers(operador(1)), versao(1)).andExpect(status().isOk());
        enviar(patch(caminho + "/cancelar").headers(morador(1)), """
                {"versao":2,"motivo":"Já não preciso"}
                """).andExpect(status().isConflict());
        enviar(patch(caminho + "/reagendar").headers(operador(1)), """
                {"versao":2,"dataAgendamento":"%s","motivo":"Sem acesso","detalhesTentativa":"Portão fechado"}
                """.formatted(futuro)).andExpect(status().isOk())
                .andExpect(jsonPath("$.historico[3].numeroTentativa").value(1))
                .andExpect(jsonPath("$.historico[3].detalhesTentativa").value("Portão fechado"));
        enviar(patch(caminho + "/iniciar").headers(operador(1)), versao(3)).andExpect(status().isOk());
        enviar(patch(caminho + "/concluir").headers(operador(1)), concluir(0, 0)).andExpect(status().isBadRequest());
        OffsetDateTime inicio = OffsetDateTime.now().minusSeconds(1);
        enviar(patch(caminho + "/concluir").headers(operador(1)), concluir(1.25, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitacao.situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.historico[5].numeroTentativa").value(2))
                .andExpect(jsonPath("$.historico[5].responsavelId").value(1));
        enviar(patch(caminho + "/iniciar").headers(operador(1)), versao(5)).andExpect(status().isConflict());
        mvc.perform(get("/api/indicadores").headers(admin()).param("inicio", inicio.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recolhidosPorMaterial[0].quantidade").value(1.25))
                .andExpect(jsonPath("$.recolhidosPorMaterial[1].quantidade").value(0));
        mvc.perform(get("/api/indicadores").headers(admin()).param("inicio", "2000-01-02T00:00:00Z")
                .param("fim", "2000-01-01T00:00:00Z")).andExpect(status().isBadRequest());
    }

    @Test
    void duasMutacoesComMesmaVersaoGeramUmUnicoEvento() throws Exception {
        long id = id(obter(criar(1, 1, enderecoDemo(false), """
                [{"materialId":1,"quantidadeEstimada":2}]
                """), 201));
        var barreira = new CountDownLatch(2);
        String caminho = "/api/solicitacoes/" + id + "/reagendar";
        String futuro = OffsetDateTime.now().plusDays(3).toString();
        enviar(patch("/api/solicitacoes/" + id + "/agendar").headers(operador(1)), agenda(0, futuro))
                .andExpect(status().isOk());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarefas = List.of("Primeira operação", "Segunda operação").stream().map(motivo -> executor.submit(() -> {
                barreira.countDown();
                if (!barreira.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("As duas requisições não alcançaram a barreira.");
                }
                return enviar(patch(caminho).headers(operador(1)), """
                        {"versao":1,"dataAgendamento":"%s","motivo":"%s"}
                        """.formatted(futuro, motivo)).andReturn().getResponse();
            })).toList();
            var primeira = tarefas.get(0).get(10, TimeUnit.SECONDS);
            var segunda = tarefas.get(1).get(10, TimeUnit.SECONDS);
            assertThat(List.of(primeira.getStatus(), segunda.getStatus())).containsExactlyInAnyOrder(200, 409);
            var conflito = primeira.getStatus() == 409 ? primeira : segunda;
            assertThat(json.readTree(conflito.getContentAsByteArray()).get("codigo").asText()).isEqualTo("CONFLITO_VERSAO");
        }
        mvc.perform(get("/api/solicitacoes/" + id).headers(morador(1))).andExpect(status().isOk())
                .andExpect(jsonPath("$.historico.length()").value(3))
                .andExpect(jsonPath("$.solicitacao.versao").value(2));
    }

    private ResultActions criar(long morador, long ponto, long endereco, String itens) throws Exception {
        return enviar(post("/api/solicitacoes").headers(morador(morador)), """
                {"pontoId":%d,"enderecoId":%d,"itens":%s}
                """.formatted(ponto, endereco, itens));
    }

    private String versao(int versao) { return """
            {"versao":%d}
            """.formatted(versao); }

    private String agenda(int versao, String data) { return """
            {"versao":%d,"dataAgendamento":"%s"}
            """.formatted(versao, data); }

    private String concluir(double papel, double plastico) { return """
            {"versao":4,"itens":[{"materialId":1,"quantidadeEfetiva":%s},{"materialId":2,"quantidadeEfetiva":%s}]}
            """.formatted(papel, plastico); }

    private org.springframework.http.HttpHeaders admin() { return ator("ADMINISTRADOR", null); }
    private org.springframework.http.HttpHeaders morador(long id) { return ator("MORADOR", id); }
    private org.springframework.http.HttpHeaders operador(long id) { return ator("OPERADOR", id); }

    private org.springframework.http.HttpHeaders ator(String tipo, Long id) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Ator-Tipo", tipo);
        if (id != null) headers.set("X-Ator-Id", id.toString());
        return headers;
    }

    private ResultActions enviar(MockHttpServletRequestBuilder request, String corpo) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private JsonNode obter(ResultActions resultado, int codigo) throws Exception {
        return json.readTree(resultado.andExpect(status().is(codigo)).andReturn().getResponse().getContentAsByteArray());
    }

    private long id(JsonNode resposta) { return resposta.at("/solicitacao/id").asLong(); }

    private long enderecoDemo(boolean semNumero) {
        return jdbc.queryForObject("select id from endereco where morador_id = 1 and sem_numero = ?",
                Long.class, semNumero);
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("select count(*) from " + tabela, Long.class);
    }
}
