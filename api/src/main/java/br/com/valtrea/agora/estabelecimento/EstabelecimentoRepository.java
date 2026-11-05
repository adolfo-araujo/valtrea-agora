package br.com.valtrea.agora.estabelecimento;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, UUID> {

	List<Estabelecimento> findByDonoIdOrderByCriadoEmAsc(UUID donoId);

	long countByDonoId(UUID donoId);

	/** Aprovados, filtrando por estado, tipo e um trecho da cidade ou do bairro (sem acento). */
	@Query("select e from Estabelecimento e where e.status = br.com.valtrea.agora.estabelecimento.Estabelecimento.Status.APROVADO "
			+ "and (:uf is null or e.uf = :uf) and (:tipo is null or e.tipo = :tipo) "
			+ "and e.busca like concat('%', :termo, '%') order by e.nome")
	List<Estabelecimento> buscar(@Param("uf") String uf, @Param("tipo") Tipo tipo, @Param("termo") String termo,
			Limit limite);

	List<Estabelecimento> findByStatusOrderByAtualizadoEmAsc(Estabelecimento.Status status);

	long countByStatus(Estabelecimento.Status status);

}
