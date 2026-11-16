package com.nowait.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ArmazenamentoArquivoService {

    @Value("${nowait.upload.diretorio}")
    private String diretorioBase;

    public String salvar(String subpasta, String nomeArquivo, byte[] conteudo) {
        try {
            Path diretorio = Path.of(diretorioBase, subpasta);
            Files.createDirectories(diretorio);
            Files.write(diretorio.resolve(nomeArquivo), conteudo);
            return "/uploads/" + subpasta + "/" + nomeArquivo;
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o arquivo enviado", e);
        }
    }

    public void remover(String subpasta, String nomeArquivo) {
        try {
            Files.deleteIfExists(Path.of(diretorioBase, subpasta, nomeArquivo));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível remover o arquivo", e);
        }
    }
}
