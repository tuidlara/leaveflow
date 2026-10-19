package com.arthur.leaveflow.service;

import com.arthur.leaveflow.dto.LeaveRequestRequest;
import com.arthur.leaveflow.dto.LeaveRequestResponse;
import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.LeaveRequest;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import com.arthur.leaveflow.exception.EmployeeNotFoundException;
import com.arthur.leaveflow.exception.InvalidLeaveRequestException;
import com.arthur.leaveflow.exception.LeaveRequestNotFoundException;
import com.arthur.leaveflow.repository.EmployeeRepository;
import com.arthur.leaveflow.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    @Test
    void naoDeveAprovarSolicitacaoQueNaoEstaPendente() {
        Employee employee = new Employee();
        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        solicitacao.aprovar("Gestor");

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.aprovarSolicitacao(1L, "Gestor")
        );
    }

    @Test
    void deveAprovarSolicitacaoPendente() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(leaveRequestRepository.save(solicitacao))
                .thenReturn(solicitacao);

        LeaveRequestResponse resposta =
                leaveRequestService.aprovarSolicitacao(1L, "Gestor");

        assertEquals(LeaveStatus.APPROVED, resposta.status());
        assertEquals("Gestor", resposta.decidedBy());
        assertNotNull(resposta.decidedAt());
    }

    @Test
    void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
        when(leaveRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                LeaveRequestNotFoundException.class,
                () -> leaveRequestService.aprovarSolicitacao(999L, "Gestor")
        );
    }

    @Test
    void naoDeveAprovarFeriasComDatasSobrepostas() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.VACATION,
                LocalDate.of(2027, 5, 15),
                LocalDate.of(2027, 5, 25),
                "Viagem"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(leaveRequestRepository.existsOverlappingRequest(
                employee.getId(),
                LeaveType.VACATION,
                LeaveStatus.APPROVED,
                LocalDate.of(2027, 5, 15),
                LocalDate.of(2027, 5, 25),
                solicitacao.getId()
        )).thenReturn(true);

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.aprovarSolicitacao(1L, "Gestor")
        );
    }

    @Test
    void deveAprovarFeriasQuandoNaoExisteSobreposicao() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.VACATION,
                LocalDate.of(2027, 5, 21),
                LocalDate.of(2027, 5, 30),
                "Viagem"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(leaveRequestRepository.existsOverlappingRequest(
                employee.getId(),
                LeaveType.VACATION,
                LeaveStatus.APPROVED,
                solicitacao.getStartDate(),
                solicitacao.getEndDate(),
                solicitacao.getId()
        )).thenReturn(false);

        when(leaveRequestRepository.save(solicitacao))
                .thenReturn(solicitacao);

        LeaveRequestResponse resposta =
                leaveRequestService.aprovarSolicitacao(1L, "Gestor");

        assertEquals(LeaveStatus.APPROVED, resposta.status());
        assertEquals("Gestor", resposta.decidedBy());
        assertNotNull(resposta.decidedAt());
    }

    @Test
    void naoDeveRejeitarSolicitacaoQueJaFoiAprovada() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
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
                        1L, "Gestor", "Motivo do teste"
                )
        );
    }

    @Test
    void deveRejeitarSolicitacaoPendente() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
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
                        1L, "Gestor", "Período indisponível"
                );

        assertEquals(LeaveStatus.REJECTED, resposta.status());
        assertEquals("Gestor", resposta.decidedBy());
        assertEquals("Período indisponível", resposta.decisionReason());
        assertNotNull(resposta.decidedAt());
    }

    @Test
    void naoDeveRejeitarSolicitacaoSemMotivo() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.rejeitarSolicitacao(
                        1L, "Gestor", " "
                )
        );
    }

    @Test
    void naoDeveAprovarSolicitacaoCancelada() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 12),
                "Consulta médica"
        );

        solicitacao.cancelar();

        when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.aprovarSolicitacao(1L, "Gestor")
        );
    }

    @Test
    void deveCancelarSolicitacaoPendente() {
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
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
        Employee employee = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                employee,
                LeaveType.ABSENCE,
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

        LeaveRequestRequest dto = new LeaveRequestRequest(
                1L,
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(funcionario));

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
    void naoDeveCriarSolicitacaoComDatasInvalidas() {
        Employee funcionario = new Employee();

        LeaveRequestRequest dto = new LeaveRequestRequest(
                1L,
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 10),
                "Férias anuais"
        );

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(funcionario));

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> leaveRequestService.criarSolicitacao(dto)
        );

        verify(leaveRequestRepository, never())
                .save(any(LeaveRequest.class));
    }

    @Test
    void naoDeveCriarSolicitacaoQuandoFuncionarioNaoExiste() {
        LeaveRequestRequest dto = new LeaveRequestRequest(
                1L,
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EmployeeNotFoundException.class,
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
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
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
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
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
        assertEquals(LeaveType.VACATION, resposta.getContent().get(0).leaveType());
        assertEquals(LeaveStatus.PENDING, resposta.getContent().get(0).status());

        verify(leaveRequestRepository).findAll(pageable);
    }

    @Test
    void deveListarSolicitacoesPorStatus() {
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        Pageable pageable = PageRequest.of(0, 10);

        Page<LeaveRequest> pagina =
                new PageImpl<>(List.of(solicitacao), pageable, 1);

        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING, pageable))
                .thenReturn(pagina);

        Page<LeaveRequestResponse> resposta =
                leaveRequestService.listarSolicitacoes(
                        pageable, null, LeaveStatus.PENDING
                );

        assertEquals(1, resposta.getTotalElements());
        assertEquals(LeaveStatus.PENDING, resposta.getContent().get(0).status());

        verify(leaveRequestRepository).findByStatus(LeaveStatus.PENDING, pageable);
    }

    @Test
    void deveListarSolicitacoesPorTipo() {
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        Pageable pageable = PageRequest.of(0, 10);

        Page<LeaveRequest> pagina =
                new PageImpl<>(List.of(solicitacao), pageable, 1);

        when(leaveRequestRepository.findByLeaveType(LeaveType.VACATION, pageable))
                .thenReturn(pagina);

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
        Employee funcionario = new Employee();

        LeaveRequest solicitacao = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 20),
                "Férias anuais"
        );

        Pageable pageable = PageRequest.of(0, 10);

        Page<LeaveRequest> pagina =
                new PageImpl<>(List.of(solicitacao), pageable, 1);

        when(leaveRequestRepository.findByStatusAndLeaveType(
                LeaveStatus.PENDING, LeaveType.VACATION, pageable
        )).thenReturn(pagina);

        Page<LeaveRequestResponse> resposta =
                leaveRequestService.listarSolicitacoes(
                        pageable, LeaveType.VACATION, LeaveStatus.PENDING
                );

        assertEquals(1, resposta.getTotalElements());
        assertEquals(LeaveType.VACATION, resposta.getContent().get(0).leaveType());
        assertEquals(LeaveStatus.PENDING, resposta.getContent().get(0).status());

        verify(leaveRequestRepository).findByStatusAndLeaveType(
                LeaveStatus.PENDING, LeaveType.VACATION, pageable
        );
    }
}