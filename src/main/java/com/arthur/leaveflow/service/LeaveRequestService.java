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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.arthur.leaveflow.repository.UserRepository;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado."
                ));

        Employee employee = user.getEmployee();

        if (employee == null) {
            throw new InvalidLeaveRequestException(
                    "O usuário não está vinculado a um funcionário."
            );
        }

        if (!request.startDate().isBefore(request.endDate())) {
            throw new InvalidLeaveRequestException(
                    "A data inicial deve ser anterior à data final."
            );
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado."
                ));

        if (user.getRole() == Role.ADMIN) {
            return toResponse(leaveRequest);
        }

        Employee employee = user.getEmployee();

        if (employee == null
                || !employee.getId().equals(leaveRequest.getEmployee().getId())) {
            throw new AccessDeniedException(
                    "Você não tem permissão para consultar esta solicitação."
            );
        }

        return toResponse(leaveRequest);

    }

    public Page<LeaveRequestResponse> listarSolicitacoes(
            Pageable pageable,
            LeaveType leaveType,
            LeaveStatus status
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado."
                ));

        Page<LeaveRequest> solicitacoes;

        if (user.getRole() == Role.ADMIN) {
            if (leaveType != null && status != null) {
                solicitacoes = leaveRequestRepository.findByStatusAndLeaveType(
                        status, leaveType, pageable);
            } else if (status != null) {
                solicitacoes = leaveRequestRepository.findByStatus(status, pageable);
            } else if (leaveType != null) {
                solicitacoes = leaveRequestRepository.findByLeaveType(leaveType, pageable);
            } else {
                solicitacoes = leaveRequestRepository.findAll(pageable);
            }
        } else {
            Employee employee = user.getEmployee();

            if (employee == null) {
                throw new AccessDeniedException(
                        "O usuário não está vinculado a um funcionário."
                );
            }

            Long employeeId = employee.getId();

            if (leaveType != null && status != null) {
                solicitacoes = leaveRequestRepository.findByEmployeeIdAndStatusAndLeaveType(
                        employeeId, status, leaveType, pageable);
            } else if (status != null) {
                solicitacoes = leaveRequestRepository.findByEmployeeIdAndStatus(
                        employeeId, status, pageable);
            } else if (leaveType != null) {
                solicitacoes = leaveRequestRepository.findByEmployeeIdAndLeaveType(
                        employeeId, leaveType, pageable);
            } else {
                solicitacoes = leaveRequestRepository.findByEmployeeId(
                        employeeId, pageable);
            }
        }

        return solicitacoes.map(this::toResponse);
    }


    public LeaveRequestResponse cancelarSolicitacao(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException(
                        "Solicitação não encontrada para o ID informado."
                ));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado."
                ));

        if (user.getRole() != Role.ADMIN) {
            Employee employee = user.getEmployee();

            if (employee == null
                    || !employee.getId().equals(leaveRequest.getEmployee().getId())) {
                throw new AccessDeniedException(
                        "Você não tem permissão para cancelar esta solicitação."
                );
            }
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException(
                    "Solicitação só pode ser cancelada enquanto pendente."
            );
        }

        leaveRequest.cancelar();
        LeaveRequest solicitacaoSalva = leaveRequestRepository.save(leaveRequest);
        return toResponse(solicitacaoSalva);
    }


    public LeaveRequestResponse aprovarSolicitacao(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Solicitação não encontrada para o ID informado."));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado."
                ));

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERVISOR) {
            throw new AccessDeniedException("Você não tem permissão para aprovar solicitações.");
        }

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
        leaveRequest.aprovar(authentication.getName());

        leaveRequestRepository.save(leaveRequest);

        return toResponse(leaveRequest);
    }

    public LeaveRequestResponse rejeitarSolicitacao(Long id, String decisionReason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Solicitação não encontrada para o ID informado."));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário autenticado não encontrado."
                ));


        if (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERVISOR) {
            throw new AccessDeniedException("Você não tem permissão para rejeitar solicitações.");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException("Somente solicitações pendentes podem ser rejeitadas.");
        }

        if (decisionReason == null || decisionReason.isBlank()) {
            throw new InvalidLeaveRequestException("O motivo da rejeição é obrigatório.");
        }
        leaveRequest.rejeitar(authentication.getName(), decisionReason);

        leaveRequestRepository.save(leaveRequest);

        return toResponse(leaveRequest);

    }

}
