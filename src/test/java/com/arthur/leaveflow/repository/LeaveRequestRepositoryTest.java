package com.arthur.leaveflow.repository;

import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.LeaveRequest;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LeaveRequestRepositoryTest {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void deveImpedirAtualizacaoComVersaoDesatualizada() {
        Employee funcionario = employeeRepository.save(new Employee(
                "Funcionário Teste",
                "concorrencia@example.com",
                "TI"
        ));

        LeaveRequest solicitacao = new LeaveRequest(
                funcionario,
                LeaveType.ABSENCE,
                LocalDate.of(2027, 5, 10),
                LocalDate.of(2027, 5, 10),
                "Consulta médica"
        );

        solicitacao = leaveRequestRepository.saveAndFlush(solicitacao);

        Long id = solicitacao.getId();

        // supervisor A carrega a solicitação
        LeaveRequest supervisorA = leaveRequestRepository.findById(id).orElseThrow();
        entityManager.detach(supervisorA);

        // supervisor B carrega a mesma solicitação
        LeaveRequest supervisorB = leaveRequestRepository.findById(id).orElseThrow();
        entityManager.detach(supervisorB);

        // ambos possuem a mesma versão e status PENDING

        supervisorA.aprovar("Supervisor A");
        supervisorB.rejeitar("Supervisor B", "Motivo de teste");

        // a primeira atualização é persistida
        leaveRequestRepository.saveAndFlush(supervisorA);

        // a segunda deve falhar por estar usando uma versão antiga
        assertThrows(
                OptimisticLockingFailureException.class,
                () -> leaveRequestRepository.saveAndFlush(supervisorB)
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveAguardarLiberacaoDoBloqueioDoFuncionario() throws Exception {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        Long funcionarioId = transactionTemplate.execute(status ->
                employeeRepository.save(new Employee(
                        "Funcionário Concorrência",
                        "funcionario.lock@example.com",
                        "TI"
                )).getId()
        );

        CountDownLatch bloqueioAdquirido = new CountDownLatch(1);
        CountDownLatch liberarBloqueio = new CountDownLatch(1);
        CountDownLatch segundaTransacaoIniciada = new CountDownLatch(1);
        CountDownLatch segundoBloqueioAdquirido = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // transação A adquire o bloqueio e o mantém
            var transacaoA = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        employeeRepository.findByIdWithLock(funcionarioId)
                                .orElseThrow();

                        bloqueioAdquirido.countDown();

                        try {
                            liberarBloqueio.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    })
            );

            assertTrue(bloqueioAdquirido.await(5, TimeUnit.SECONDS));

            // transação B tenta adquirir o mesmo bloqueio
            var transacaoB = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        segundaTransacaoIniciada.countDown();

                        employeeRepository.findByIdWithLock(funcionarioId)
                                .orElseThrow();

                        segundoBloqueioAdquirido.countDown();
                    })
            );

            assertTrue(segundaTransacaoIniciada.await(5, TimeUnit.SECONDS));

            // B ainda não conseguiu adquirir o bloqueio
            assertFalse(segundoBloqueioAdquirido.await(1, TimeUnit.SECONDS));

            // A libera o bloqueio ao finalizar a transação
            liberarBloqueio.countDown();

            transacaoA.get(5, TimeUnit.SECONDS);
            transacaoB.get(5, TimeUnit.SECONDS);

            // B finalmente conseguiu adquirir o bloqueio
            assertTrue(segundoBloqueioAdquirido.await(5, TimeUnit.SECONDS));

        } finally {
            liberarBloqueio.countDown();
            executor.shutdownNow();
        }
    }
}
