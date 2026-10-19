package com.arthur.leaveflow.service;

import com.arthur.leaveflow.dto.LeaveRequestResponse;
import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.LeaveRequest;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import com.arthur.leaveflow.exception.InvalidLeaveRequestException;
import com.arthur.leaveflow.exception.LeaveRequestNotFoundException;
import com.arthur.leaveflow.repository.EmployeeRepository;
import com.arthur.leaveflow.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
}