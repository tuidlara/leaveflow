package com.arthur.leaveflow.controller;

import com.arthur.leaveflow.dto.LeaveRequestRequest;
import com.arthur.leaveflow.dto.LeaveRequestResponse;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import com.arthur.leaveflow.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestResponse createLeaveRequest(@Valid @RequestBody LeaveRequestRequest request) {
        return leaveRequestService.criarSolicitacao(request);
    }

    @GetMapping("/{id}")
    public LeaveRequestResponse findLeaveRequestById(@PathVariable Long id) {
        return leaveRequestService.buscarSolicitacaoPorId(id);
    }

    @GetMapping
    public Page<LeaveRequestResponse> listLeaveRequests(
            Pageable pageable,
            @RequestParam(required = false) LeaveType leaveType,
            @RequestParam(required = false) LeaveStatus status) {
        return leaveRequestService.listarSolicitacoes(pageable, leaveType, status);
    }

    @PatchMapping("/{id}/cancel")
    public LeaveRequestResponse cancelRequest(@PathVariable Long id) {
        return leaveRequestService.cancelarSolicitacao(id);
    }

    @PatchMapping("/{id}/approve")
    public LeaveRequestResponse approveRequest(
            @PathVariable Long id,
            @RequestParam String decidedBy) {
        return leaveRequestService.aprovarSolicitacao(id, decidedBy);
    }

    @PatchMapping("/{id}/reject")
    public LeaveRequestResponse rejectRequest(
            @PathVariable Long id,
            @RequestParam String decidedBy,
            @RequestParam String decisionReason) {
        return leaveRequestService.rejeitarSolicitacao(id, decidedBy, decisionReason);
    }

}
