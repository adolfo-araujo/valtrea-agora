package br.com.valtrea.agora.estabelecimento;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DenunciaRepository extends JpaRepository<Denuncia, Denuncia.Chave> {

	long countByIdEstabelecimentoId(UUID estabelecimentoId);

	List<Denuncia> findByIdEstabelecimentoId(UUID estabelecimentoId);

	List<Denuncia> findByIdUsuarioId(UUID usuarioId);

	@Modifying
	@Query("delete from Denuncia d where d.id.estabelecimentoId = :estabelecimento")
	void apagarDo(@Param("estabelecimento") UUID estabelecimentoId);

}
