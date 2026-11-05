package br.com.valtrea.agora.agenda;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

	@Query("select r from Reserva r join fetch r.horario h join fetch h.estabelecimento where r.cliente.id = :cliente "
			+ "order by h.inicio desc")
	List<Reserva> doCliente(@Param("cliente") UUID clienteId);

	@Query("select count(r) from Reserva r where r.cliente.id = :cliente "
			+ "and r.status = br.com.valtrea.agora.agenda.Reserva.Status.CONFIRMADA and r.horario.inicio > :agora")
	long confirmadasFuturas(@Param("cliente") UUID clienteId, @Param("agora") Instant agora);

	@Query("select r from Reserva r join fetch r.cliente where r.horario.id in :horarios "
			+ "and r.status <> br.com.valtrea.agora.agenda.Reserva.Status.CANCELADA_CLIENTE")
	List<Reserva> dosHorarios(@Param("horarios") Collection<UUID> horarioIds);

	Optional<Reserva> findFirstByHorarioIdAndStatus(UUID horarioId, Reserva.Status status);

}
