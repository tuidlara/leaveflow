package com.arthur.leaveflow.service;

import com.arthur.leaveflow.dto.EmployeeResponse;
import com.arthur.leaveflow.dto.LeaveRequestRequest;
import com.arthur.leaveflow.dto.LeaveRequestResponse;
import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.LeaveRequest;
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

    public Page<LeaveRequestResponse> listarSolicitacoes(Pageable pageable) {
        Page<LeaveRequest> solicitacoes;
        solicitacoes = leaveRequestRepository.findAll(pageable);
        return solicitacoes.map(this::toResponse);
    }

}
