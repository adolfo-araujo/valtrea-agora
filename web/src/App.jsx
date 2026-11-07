import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './lib/auth'
import { AvisoTermos } from './components/aviso-termos'
import { Pagina } from './components/ui'
import { InicioPage } from './pages/inicio'
import { NegociosPage } from './pages/negocios'
import { CadastroPage, ConfirmarEmailPage, EntrarPage, EsqueciSenhaPage } from './pages/auth'
import { EstabelecimentoPage } from './pages/estabelecimento'
import { ReservasPage } from './pages/reservas'
import { AgendaPage, NegocioPage } from './pages/negocio'
import { ContaPage } from './pages/conta'
import { AcessibilidadePage, ExcluirContaPage, PrivacidadePage, TermosPage } from './pages/legal'
import { AjudaPage } from './pages/ajuda'
import { NovidadesPage } from './pages/novidades'

function Protegida({ children }) {
  const { perfil, carregando } = useAuth()
  if (carregando) return <Pagina><p className="text-muted">Carregando…</p></Pagina>
  if (!perfil) return <Navigate to="/entrar" replace />
  return children
}

export default function App() {
  return (
    <>
      <AvisoTermos />
      <Routes>
        <Route path="/" element={<InicioPage />} />
        <Route path="/entrar" element={<EntrarPage />} />
        <Route path="/cadastro" element={<CadastroPage />} />
        <Route path="/esqueci-senha" element={<EsqueciSenhaPage />} />
        <Route path="/confirmar-email" element={<ConfirmarEmailPage />} />
        <Route path="/para-negocios" element={<NegociosPage />} />
        <Route path="/e/:id" element={<EstabelecimentoPage />} />
        <Route path="/reservas" element={<Protegida><ReservasPage /></Protegida>} />
        <Route path="/negocio" element={<Protegida><NegocioPage /></Protegida>} />
        <Route path="/negocio/:id" element={<Protegida><AgendaPage /></Protegida>} />
        <Route path="/conta" element={<Protegida><ContaPage /></Protegida>} />
        <Route path="/novidades" element={<NovidadesPage />} />
        <Route path="/ajuda" element={<AjudaPage />} />
        <Route path="/termos" element={<TermosPage />} />
        <Route path="/privacidade" element={<PrivacidadePage />} />
        <Route path="/acessibilidade" element={<AcessibilidadePage />} />
        <Route path="/excluir-conta" element={<ExcluirContaPage />} />
        <Route path="*" element={<Pagina titulo="Página não encontrada"><p className="text-muted">Confira o endereço.</p></Pagina>} />
      </Routes>
    </>
  )
}
