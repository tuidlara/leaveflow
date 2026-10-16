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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    private LeaveRequestResponse toResponse(LeaveRequest request) {
        return new LeaveRequestResponse(
                request.getId(),
                request.getEmployee().getId(),
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getDecisionReason(),
                request.getDecidedBy(),
                request.getDecidedAt()
        );
    }

    public LeaveRequestResponse criarSolicitacao(LeaveRequestRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "ID de funcionário não encontrado"
                ));

        if (!request.startDate().isBefore(request.endDate())) {
            throw new InvalidLeaveRequestException("A data inicial deve ser anterior à data final.");
        }

        LeaveRequest leaveRequest = new LeaveRequest(
                employee,
                request.leaveType(),
                request.startDate(),
                request.endDate(),
                request.reason()
        );

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        return toResponse(savedRequest);
    }

    public LeaveRequestResponse buscarSolicitacaoPorId(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Solicitação não encontrada para o ID informado."));
        return toResponse(leaveRequest);

    }

    public Page<LeaveRequestResponse> listarSolicitacoes(Pageable pageable, LeaveType leaveType, LeaveStatus status) {
        Page<LeaveRequest> solicitacoes;
        if (leaveType != null && status != null) {
            solicitacoes = leaveRequestRepository.findByStatusAndLeaveType(status, leaveType, pageable);
        } else if (status != null) {
            solicitacoes = leaveRequestRepository.findByStatus(status, pageable);
        } else if (leaveType != null) {
            solicitacoes = leaveRequestRepository.findByLeaveType(leaveType, pageable);
        } else {
            solicitacoes = leaveRequestRepository.findAll(pageable);
        }

        return solicitacoes.map(this::toResponse);
    }

    public LeaveRequestResponse cancelarSolicitacao(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Solicitação não encontrada para o ID informado."));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException("Solicitação só pode ser cancelada enquanto pendente");
        }
        leaveRequest.cancelar();

        leaveRequestRepository.save(leaveRequest);

        return toResponse(leaveRequest);
    }

    public LeaveRequestResponse aprovarSolicitacao(Long id, String decidedBy) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Solicitação não encontrada para o ID informado."));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException("Somente solicitações pendentes podem ser aprovadas.");
        }

        if (leaveRequest.getLeaveType() == LeaveType.VACATION) {
            boolean hasOverlap = leaveRequestRepository.existsOverlappingRequest(
                    leaveRequest.getEmployee().getId(),
                    LeaveType.VACATION,
                    LeaveStatus.APPROVED,
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate(),
                    leaveRequest.getId()
            );

            if (hasOverlap) {
                throw new InvalidLeaveRequestException(
                        "As férias não podem ser aprovadas porque se sobrepõem a outras férias já aprovadas."
                );
            }
        }
        leaveRequest.aprovar(decidedBy);

        leaveRequestRepository.save(leaveRequest);

        return toResponse(leaveRequest);
    }

    public LeaveRequestResponse rejeitarSolicitacao(Long id, String decidedBy, String decisionReason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Solicitação não encontrada para o ID informado."));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException("Somente solicitações pendentes podem ser rejeitadas.");
        }

        if (decisionReason == null || decisionReason.isBlank()) {
            throw new InvalidLeaveRequestException("O motivo da rejeição é obrigatório.");
        }
        leaveRequest.rejeitar(decidedBy, decisionReason);

        leaveRequestRepository.save(leaveRequest);

        return toResponse(leaveRequest);

    }

}
