package com.coletaseletiva.repository;

import java.util.Optional;
import com.coletaseletiva.entity.Endereco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    Optional<Endereco> findByIdAndMoradorId(long id, long moradorId);
    Page<Endereco> findByMoradorId(long moradorId, Pageable pageable);
}
