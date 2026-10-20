
package com.arthur.leaveflow.repository;

import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.LeaveRequest;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class LeaveRequestRepositoryTest {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void deveEncontrarFeriasAprovadasComDatasSobrepostas() {
        Employee funcionario = new Employee(
                "Funcionário de Teste",
                "funcionario.teste@example.com",
                "TI"
        );
        funcionario = employeeRepository.save(funcionario);

        LeaveRequest feriasExistentes = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 20),
                "Férias existentes"
        );
        feriasExistentes.aprovar("Gestor");
        leaveRequestRepository.saveAndFlush(feriasExistentes);

        boolean sobreposicao =
                leaveRequestRepository.existsOverlappingRequest(
                        funcionario.getId(),
                        LeaveType.VACATION,
                        LeaveStatus.APPROVED,
                        LocalDate.of(2027, 5, 15),
                        LocalDate.of(2027, 5, 25),
                        -1L
                );

        assertTrue(sobreposicao);
    }

    @Test
    void naoDeveEncontrarFeriasQuandoAsDatasNaoSeSobrepoem() {
        Employee funcionario = new Employee(
                "Funcionário de Teste",
                "funcionario.teste@example.com",
                "TI"
        );
        funcionario = employeeRepository.save(funcionario);

        LeaveRequest feriasExistentes = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 20),
                "Férias existentes"
        );
        feriasExistentes.aprovar("Gestor");
        leaveRequestRepository.saveAndFlush(feriasExistentes);

        boolean sobreposicao =
                leaveRequestRepository.existsOverlappingRequest(
                        funcionario.getId(),
                        LeaveType.VACATION,
                        LeaveStatus.APPROVED,
                        LocalDate.of(2027, 5, 21),
                        LocalDate.of(2027, 5, 30),
                        -1L
                );

        assertFalse(sobreposicao);
    }

    @Test
    void naoDeveEncontrarSobreposicaoParaOutroFuncionario() {
        Employee funcionario1 = employeeRepository.save(new Employee(
                "Funcionário Um",
                "funcionario.um@example.com",
                "TI"
        ));

        Employee funcionario2 = employeeRepository.save(new Employee(
                "Funcionário Dois",
                "funcionario.dois@example.com",
                "TI"
        ));

        LeaveRequest ferias = new LeaveRequest(
                funcionario1,
                LeaveType.VACATION,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 20),
                "Férias do funcionário um"
        );
        ferias.aprovar("Gestor");
        leaveRequestRepository.saveAndFlush(ferias);

        boolean sobreposicao =
                leaveRequestRepository.existsOverlappingRequest(
                        funcionario2.getId(),
                        LeaveType.VACATION,
                        LeaveStatus.APPROVED,
                        LocalDate.of(2027, 5, 15),
                        LocalDate.of(2027, 5, 25),
                        -1L
                );

        assertFalse(sobreposicao);
    }

    @Test
    void naoDeveEncontrarSobreposicaoComAPropriaSolicitacao() {
        Employee funcionario = employeeRepository.save(new Employee(
                "Funcionário de Teste",
                "funcionario.proprio@example.com",
                "TI"
        ));

        LeaveRequest ferias = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 20),
                "Férias existentes"
        );
        ferias.aprovar("Gestor");
        ferias = leaveRequestRepository.saveAndFlush(ferias);

        boolean sobreposicao =
                leaveRequestRepository.existsOverlappingRequest(
                        funcionario.getId(),
                        LeaveType.VACATION,
                        LeaveStatus.APPROVED,
                        LocalDate.of(2027, 5, 10),
                        LocalDate.of(2027, 5, 20),
                        ferias.getId()
                );

        assertFalse(sobreposicao);
    }

    @Test
    void naoDeveEncontrarSobreposicaoParaFeriasPendentes() {
        Employee funcionario = employeeRepository.save(new Employee(
                "Funcionário de Teste",
                "funcionario.pendente@example.com",
                "TI"
        ));

        LeaveRequest ferias = new LeaveRequest(
                funcionario,
                LeaveType.VACATION,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 20),
                "Férias pendentes"
        );
        leaveRequestRepository.saveAndFlush(ferias);

        boolean sobreposicao =
                leaveRequestRepository.existsOverlappingRequest(
                        funcionario.getId(),
                        LeaveType.VACATION,
                        LeaveStatus.APPROVED,
                        LocalDate.of(2027, 5, 15),
                        LocalDate.of(2027, 5, 25),
                        -1L
                );

        assertFalse(sobreposicao);
    }

}
