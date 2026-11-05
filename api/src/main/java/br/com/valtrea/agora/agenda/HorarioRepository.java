package br.com.valtrea.agora.agenda;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.valtrea.agora.estabelecimento.Tipo;
import jakarta.persistence.LockModeType;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {

	/** Trava a linha: dois cliques em "Reservar" ao mesmo tempo não reservam o mesmo horário. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select h from Horario h where h.id = :id")
	Optional<Horario> travar(@Param("id") UUID id);

	/** Horários livres de estabelecimentos aprovados, do mais próximo para o mais distante. */
	@Query("select h from Horario h join fetch h.estabelecimento e "
			+ "where h.status = br.com.valtrea.agora.agenda.Horario.Status.LIVRE and h.inicio > :desde and h.inicio < :ate "
			+ "and e.status = br.com.valtrea.agora.estabelecimento.Estabelecimento.Status.APROVADO "
			+ "and (:uf is null or e.uf = :uf) and (:tipo is null or e.tipo = :tipo) "
			+ "and e.busca like concat('%', :termo, '%') order by h.inicio")
	List<Horario> livres(@Param("desde") Instant desde, @Param("ate") Instant ate, @Param("uf") String uf,
			@Param("tipo") Tipo tipo, @Param("termo") String termo, Limit limite);

	@Query("select h from Horario h where h.estabelecimento.id = :estabelecimento "
			+ "and h.status = br.com.valtrea.agora.agenda.Horario.Status.LIVRE and h.inicio > :desde and h.inicio < :ate "
			+ "order by h.inicio")
	List<Horario> livresDo(@Param("estabelecimento") UUID estabelecimentoId, @Param("desde") Instant desde,
			@Param("ate") Instant ate);

	/** [estabelecimentoId, quantidade] de horários livres de cada estabelecimento da lista. */
	@Query("select h.estabelecimento.id, count(h) from Horario h where h.estabelecimento.id in :ids "
			+ "and h.status = br.com.valtrea.agora.agenda.Horario.Status.LIVRE and h.inicio > :desde and h.inicio < :ate "
			+ "group by h.estabelecimento.id")
	List<Object[]> contarLivres(@Param("ids") Collection<UUID> ids, @Param("desde") Instant desde,
			@Param("ate") Instant ate);

	/** Agenda do dono: da semana passada em diante. */
	List<Horario> findByEstabelecimentoIdAndInicioAfterOrderByInicioAsc(UUID estabelecimentoId, Instant desde);

	long countByEstabelecimentoIdAndStatusAndInicioAfter(UUID estabelecimentoId, Horario.Status status, Instant desde);

}
