
package com.arthur.leaveflow.service;

import com.arthur.leaveflow.dto.LeaveRequestRequest;
import com.arthur.leaveflow.dto.LeaveRequestResponse;
import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.LeaveRequest;
import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.exception.InvalidLeaveRequestException;
import com.arthur.leaveflow.exception.LeaveRequestNotFoundException;
import com.arthur.leaveflow.repository.EmployeeRepository;
import com.arthur.leaveflow.repository.LeaveRequestRepository;
import com.arthur.leaveflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private static final String EMAIL_FUNCIONARIO = "funcionario@teste.com";
    private static final String EMAIL_ADMIN = "admin@teste.com";

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarFuncionario(Employee funcionario) {
        User usuario = new User(
                EMAIL_FUNCIONARIO,
                "senha",
                Role.EMPLOYEE,
                funcionario
        );

        when(userRepository.findByEmail(EMAIL_FUNCIONARIO))
                .thenReturn(Optional.of(usuario));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL_FUNCIONARIO,
                        null,
                        List.of(() -> "ROLE_EMPLOYEE")
                )
        );
    }

    private void autenticarAdmin() {
        User usuario = new User(
                EMAIL_ADMIN,
                "senha",
                Role.ADMIN
        );

        when(userRepository.findByEmail(EMAIL_ADMIN))
                .thenReturn(Optional.of(usuario));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL_ADMIN,
                        null,
                        List.of(() -> "ROLE_ADMIN")
                )
        );
    }

    private LeaveRequest criarSolicitacao(
            Employee funcionario,
            LeaveType tipo,
            LocalDate inicio,
            LocalDate fim,
            String motivo
    ) {
        return new LeaveRequest(funcionario, tipo, inicio, fim, motivo);
    }

    @Test
    void naoDeveAprovarSolicitacaoQueNaoEstaPendente() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        solicitacao.aprovar("Gestor");

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.aprovarSolicitacao(1L)
        );
    }

    @Test
    void deveAprovarSolicitacaoPendente() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));
        when(leaveRequestRepository.save(solicitacao))
                .thenReturn(solicitacao);

        LeaveRequestResponse resposta =
                leaveRequestService.aprovarSolicitacao(1L);

        assertEquals(LeaveStatus.APPROVED, resposta.status());
        assertEquals(EMAIL_ADMIN, resposta.decidedBy());
        assertNotNull(resposta.decidedAt());
    }

    @Test
    void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
        when(leaveRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                LeaveRequestNotFoundException.class,
                () -> leaveRequestService.aprovarSolicitacao(999L)
        );
    }

    @Test
    void naoDeveAprovarFeriasComDatasSobrepostas() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.VACATION,
                LocalDate.of(2027, 5, 15),
                LocalDate.of(2027, 5, 25),
                "Viagem"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));
        when(leaveRequestRepository.existsOverlappingRequest(
                funcionario.getId(),
                LeaveType.VACATION,
                LeaveStatus.APPROVED,
                solicitacao.getStartDate(),
                solicitacao.getEndDate(),
                solicitacao.getId()
        )).thenReturn(true);

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.aprovarSolicitacao(1L)
        );
    }

    @Test
    void deveAprovarFeriasQuandoNaoExisteSobreposicao() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.VACATION,
                LocalDate.of(2027, 5, 21),
                LocalDate.of(2027, 5, 30),
                "Viagem"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));
        when(leaveRequestRepository.existsOverlappingRequest(
                funcionario.getId(),
                LeaveType.VACATION,
                LeaveStatus.APPROVED,
                solicitacao.getStartDate(),
                solicitacao.getEndDate(),
                solicitacao.getId()
        )).thenReturn(false);
        when(leaveRequestRepository.save(solicitacao))
                .thenReturn(solicitacao);

        LeaveRequestResponse resposta =
                leaveRequestService.aprovarSolicitacao(1L);

        assertEquals(LeaveStatus.APPROVED, resposta.status());
        assertEquals(EMAIL_ADMIN, resposta.decidedBy());
        assertNotNull(resposta.decidedAt());
    }

    @Test
    void naoDeveRejeitarSolicitacaoQueJaFoiAprovada() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        solicitacao.aprovar("Gestor");

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.rejeitarSolicitacao(
                        1L, "Motivo do teste"
                )
        );
    }

    @Test
    void deveRejeitarSolicitacaoPendente() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));
        when(leaveRequestRepository.save(solicitacao))
                .thenReturn(solicitacao);

        LeaveRequestResponse resposta =
                leaveRequestService.rejeitarSolicitacao(
                        1L, "Período indisponível"
                );

        assertEquals(LeaveStatus.REJECTED, resposta.status());
        assertEquals(EMAIL_ADMIN, resposta.decidedBy());
        assertEquals("Período indisponível", resposta.decisionReason());
        assertNotNull(resposta.decidedAt());
    }

    @Test
    void naoDeveRejeitarSolicitacaoSemMotivo() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.rejeitarSolicitacao(1L, " ")
        );
    }

    @Test
    void naoDeveAprovarSolicitacaoCancelada() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        solicitacao.cancelar();

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.aprovarSolicitacao(1L)
        );
    }

    @Test
    void deveCancelarSolicitacaoPendente() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));
        when(leaveRequestRepository.save(solicitacao))
                .thenReturn(solicitacao);

        LeaveRequestResponse resposta =
                leaveRequestService.cancelarSolicitacao(1L);

        assertEquals(LeaveStatus.CANCELLED, resposta.status());
    }

    @Test
    void naoDeveCancelarSolicitacaoJaDecidida() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        solicitacao.aprovar("Gestor");

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.cancelarSolicitacao(1L)
        );
    }

    @Test
    void deveCriarSolicitacaoDeFerias() {
        Employee funcionario = new Employee();
        autenticarFuncionario(funcionario);

        LeaveRequestRequest dto = new LeaveRequestRequest(
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Viagem em família"
        );

        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestResponse resposta =
                leaveRequestService.criarSolicitacao(dto);

        assertNotNull(resposta);
        assertEquals(LeaveStatus.PENDING, resposta.status());
        assertEquals(LeaveType.VACATION, resposta.leaveType());
        assertEquals(LocalDate.of(2027, 1, 10), resposta.startDate());
        assertEquals(LocalDate.of(2027, 1, 20), resposta.endDate());

        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void deveCriarSolicitacaoDeUmDia() {
        Employee funcionario = new Employee();
        autenticarFuncionario(funcionario);

        LeaveRequestRequest dto = new LeaveRequestRequest(
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 10),
                "Viagem"
        );

        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestResponse resposta =
                leaveRequestService.criarSolicitacao(dto);

        assertNotNull(resposta);
        assertEquals(LeaveStatus.PENDING, resposta.status());
        assertEquals(LeaveType.VACATION, resposta.leaveType());
        assertEquals(LocalDate.of(2027, 1, 10), resposta.startDate());
        assertEquals(LocalDate.of(2027, 1, 10), resposta.endDate());

        verify(leaveRequestRepository).save(any(LeaveRequest.class));

    }

    @Test
    void naoDeveCriarSolicitacaoComDatasInvalidas() {
        Employee funcionario = new Employee();
        autenticarFuncionario(funcionario);

        LeaveRequestRequest dto = new LeaveRequestRequest(
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 11),
                LocalDate.of(2027, 1, 10),
                "Férias anuais"
        );

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.criarSolicitacao(dto)
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));
    }

    @Test
    void naoDeveCriarSolicitacaoQuandoUsuarioNaoEstaVinculadoAFuncionario() {
        User usuario = new User(
                EMAIL_FUNCIONARIO,
                "senha",
                Role.EMPLOYEE
        );

        when(userRepository.findByEmail(EMAIL_FUNCIONARIO))
                .thenReturn(Optional.of(usuario));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL_FUNCIONARIO,
                        null,
                        List.of(() -> "ROLE_EMPLOYEE")
                )
        );

        LeaveRequestRequest dto = new LeaveRequestRequest(
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.criarSolicitacao(dto)
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));
    }

    @Test
    void deveLancarExcecaoQuandoSolicitacaoNaoExisteAoBuscar() {
        when(leaveRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                LeaveRequestNotFoundException.class,
                () -> leaveRequestService.buscarSolicitacaoPorId(999L)
        );
    }

    @Test
    void deveBuscarSolicitacaoPorId() {
        autenticarAdmin();

        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        LeaveRequestResponse resposta =
                leaveRequestService.buscarSolicitacaoPorId(1L);

        assertNotNull(resposta);
        assertEquals(LeaveType.VACATION, resposta.leaveType());
        assertEquals(LeaveStatus.PENDING, resposta.status());
        assertEquals(LocalDate.of(2027, 1, 10), resposta.startDate());
        assertEquals(LocalDate.of(2027, 1, 20), resposta.endDate());

        verify(leaveRequestRepository).findById(1L);
    }

    @Test
    void deveListarSolicitacoesSemFiltros() {

        autenticarAdmin();
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<LeaveRequest> pagina =
                new PageImpl<>(List.of(solicitacao), pageable, 1);

        when(leaveRequestRepository.findAll(pageable))
                .thenReturn(pagina);

        Page<LeaveRequestResponse> resposta =
                leaveRequestService.listarSolicitacoes(pageable, null, null);

        assertEquals(1, resposta.getTotalElements());
        assertEquals(
                LeaveType.VACATION,
                resposta.getContent().get(0).leaveType()
        );
        assertEquals(
                LeaveStatus.PENDING,
                resposta.getContent().get(0).status()
        );

        verify(leaveRequestRepository).findAll(pageable);
    }

    @Test
    void deveListarSolicitacoesPorStatus() {

        autenticarAdmin();
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<LeaveRequest> pagina =
                new PageImpl<>(List.of(solicitacao), pageable, 1);

        when(leaveRequestRepository.findByStatus(
                LeaveStatus.PENDING, pageable
        )).thenReturn(pagina);

        Page<LeaveRequestResponse> resposta =
                leaveRequestService.listarSolicitacoes(
                        pageable, null, LeaveStatus.PENDING
                );

        assertEquals(1, resposta.getTotalElements());
        assertEquals(
                LeaveStatus.PENDING,
                resposta.getContent().get(0).status()
        );

        verify(leaveRequestRepository)
                .findByStatus(LeaveStatus.PENDING, pageable);
    }

    @Test
    void deveListarSolicitacoesPorTipo() {

        autenticarAdmin();
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<LeaveRequest> pagina =
                new PageImpl<>(List.of(solicitacao), pageable, 1);

        when(leaveRequestRepository.findByLeaveType(
                LeaveType.VACATION, pageable
        )).thenReturn(pagina);

        Page<LeaveRequestResponse> resposta =
                leaveRequestService.listarSolicitacoes(
                        pageable, LeaveType.VACATION, null
                );

        assertEquals(1, resposta.getTotalElements());
        assertEquals(
                LeaveType.VACATION,
                resposta.getContent().get(0).leaveType()
        );

        verify(leaveRequestRepository)
                .findByLeaveType(LeaveType.VACATION, pageable);
    }

    @Test
    void deveListarSolicitacoesPorTipoEStatus() {

        autenticarAdmin();
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = criarSolicitacao(
                funcionario, LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<LeaveRequest> pagina =
                new PageImpl<>(List.of(solicitacao), pageable, 1);

        when(leaveRequestRepository.findByStatusAndLeaveType(
                LeaveStatus.PENDING,
                LeaveType.VACATION,
                pageable
        )).thenReturn(pagina);

        Page<LeaveRequestResponse> resposta =
                leaveRequestService.listarSolicitacoes(
                        pageable,
                        LeaveType.VACATION,
                        LeaveStatus.PENDING
                );

        assertEquals(1, resposta.getTotalElements());
        assertEquals(
                LeaveType.VACATION,
                resposta.getContent().get(0).leaveType()
        );
        assertEquals(
                LeaveStatus.PENDING,
                resposta.getContent().get(0).status()
        );

        verify(leaveRequestRepository).findByStatusAndLeaveType(
                LeaveStatus.PENDING,
                LeaveType.VACATION,
                pageable
        );
    }
}
