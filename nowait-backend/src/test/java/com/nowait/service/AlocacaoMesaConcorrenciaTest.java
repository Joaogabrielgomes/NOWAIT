package com.nowait.service;

import com.nowait.model.Estabelecimento;
import com.nowait.model.Mesa;
import com.nowait.model.Perfil;
import com.nowait.model.StatusMesa;
import com.nowait.model.Usuario;
import com.nowait.repository.EstabelecimentoRepository;
import com.nowait.repository.MesaRepository;
import com.nowait.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, com um banco de dados real (H2 em memória, perfil "test"), que a alocação de mesas
 * não permite dupla ocupação sob concorrência: dispara N threads simultâneas pedindo a mesma
 * mesa e verifica que só uma delas efetivamente a recebe — o problema identificado na revisão
 * do orientador (AlocacaoMesaService sem lock) e corrigido com SELECT ... FOR UPDATE
 * (ver MesaRepository.buscarLivresParaAlocacaoComLock).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AlocacaoMesaConcorrenciaTest {

    private static final int QUANTIDADE_THREADS = 10;

    @Autowired private AlocacaoMesaService alocacaoMesaService;
    @Autowired private MesaRepository mesaRepository;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private String estabelecimentoId;

    @BeforeEach
    void setUp() {
        Usuario dono = usuarioRepository.save(Usuario.builder()
                .nome("Dono Teste Concorrência")
                .email("dono-concorrencia-" + System.nanoTime() + "@exemplo.com")
                .senhaHash("hash-irrelevante")
                .role(Perfil.ESTABELECIMENTO)
                .build());

        Estabelecimento estabelecimento = estabelecimentoRepository.save(Estabelecimento.builder()
                .usuario(dono)
                .nome("Restaurante Concorrência")
                .categoria("BRASILEIRA")
                .telefone("11999999999")
                .cep("01001000")
                .logradouro("Praça da Sé")
                .numero("1")
                .bairro("Sé")
                .cidade("São Paulo")
                .estado("SP")
                .build());
        estabelecimentoId = estabelecimento.getId();

        // Só uma mesa livre: é exatamente essa disputa que expõe a condição de corrida.
        mesaRepository.save(Mesa.builder()
                .estabelecimento(estabelecimento)
                .numero("1")
                .capacidade(4)
                .status(StatusMesa.LIVRE)
                .build());
    }

    @Test
    void chamadasConcorrentesParaAMesmaMesa_apenasUmaThreadConseguAlocar() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(QUANTIDADE_THREADS);
        CountDownLatch partida = new CountDownLatch(1);

        List<Callable<Optional<Mesa>>> tarefas = java.util.stream.IntStream.range(0, QUANTIDADE_THREADS)
                .<Callable<Optional<Mesa>>>mapToObj(i -> () -> {
                    partida.await();
                    return alocacaoMesaService.alocarMesa(estabelecimentoId, 2);
                })
                .collect(Collectors.toList());

        List<Future<Optional<Mesa>>> futuros = tarefas.stream().map(executor::submit).collect(Collectors.toList());
        partida.countDown(); // libera todas as threads de uma vez, maximizando a disputa pela mesma mesa

        AtomicInteger sucesso = new AtomicInteger(0);
        AtomicInteger semMesa = new AtomicInteger(0);
        for (Future<Optional<Mesa>> futuro : futuros) {
            Optional<Mesa> resultado = futuro.get(10, TimeUnit.SECONDS);
            if (resultado.isPresent()) {
                sucesso.incrementAndGet();
            } else {
                semMesa.incrementAndGet();
            }
        }
        executor.shutdown();

        assertThat(sucesso.get())
                .as("exatamente uma das %d chamadas concorrentes deveria conseguir a única mesa livre", QUANTIDADE_THREADS)
                .isEqualTo(1);
        assertThat(semMesa.get()).isEqualTo(QUANTIDADE_THREADS - 1);

        List<Mesa> mesasDoEstabelecimento = mesaRepository.findByEstabelecimentoIdOrderByNumeroAsc(estabelecimentoId);
        assertThat(mesasDoEstabelecimento).hasSize(1);
        assertThat(mesasDoEstabelecimento.get(0).getStatus()).isEqualTo(StatusMesa.OCUPADA);
    }
}
