package com.nowait.service;

import com.nowait.exception.BusinessRuleException;
import com.nowait.exception.InvalidInputException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

@Component
public class ProcessadorImagemEstabelecimento {

    private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");
    private static final float QUALIDADE_JPEG = 0.85f;

    public byte[] processar(MultipartFile arquivo, int larguraMinima, int alturaMinima, int larguraMaxima) {
        validarArquivo(arquivo);
        BufferedImage imagem = lerImagem(arquivo);
        validarDimensoes(imagem, larguraMinima, alturaMinima);

        BufferedImage redimensionada = redimensionarSeNecessario(imagem, larguraMaxima);
        return converterParaJpeg(redimensionada);
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new InvalidInputException("Nenhuma imagem foi enviada");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new InvalidInputException("A imagem deve ter no máximo 5MB");
        }
        if (!TIPOS_PERMITIDOS.contains(arquivo.getContentType())) {
            throw new InvalidInputException("Formato de imagem não suportado. Envie JPEG, PNG ou WEBP");
        }
    }

    private BufferedImage lerImagem(MultipartFile arquivo) {
        try {
            BufferedImage imagem = ImageIO.read(arquivo.getInputStream());
            if (imagem == null) {
                throw new InvalidInputException("Arquivo de imagem inválido ou corrompido");
            }
            return imagem;
        } catch (IOException e) {
            throw new InvalidInputException("Não foi possível ler a imagem enviada");
        }
    }

    private void validarDimensoes(BufferedImage imagem, int larguraMinima, int alturaMinima) {
        if (imagem.getWidth() < larguraMinima || imagem.getHeight() < alturaMinima) {
            throw new BusinessRuleException(
                    "A imagem deve ter no mínimo " + larguraMinima + "x" + alturaMinima + " pixels");
        }
    }

    private BufferedImage redimensionarSeNecessario(BufferedImage original, int larguraMaxima) {
        if (original.getWidth() <= larguraMaxima) {
            return original;
        }

        int novaAltura = (int) ((double) original.getHeight() / original.getWidth() * larguraMaxima);
        BufferedImage redimensionada = new BufferedImage(larguraMaxima, novaAltura, BufferedImage.TYPE_INT_RGB);

        Graphics2D graficos = redimensionada.createGraphics();
        graficos.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graficos.drawImage(original, 0, 0, larguraMaxima, novaAltura, null);
        graficos.dispose();

        return redimensionada;
    }

    private byte[] converterParaJpeg(BufferedImage imagem) {
        try {
            BufferedImage semTransparencia = removerTransparencia(imagem);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();

            Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter escritor = escritores.next();
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(QUALIDADE_JPEG);

            escritor.setOutput(new MemoryCacheImageOutputStream(saida));
            escritor.write(null, new IIOImage(semTransparencia, null, null), parametros);
            escritor.dispose();

            return saida.toByteArray();
        } catch (IOException e) {
            throw new InvalidInputException("Não foi possível processar a imagem enviada");
        }
    }

    private BufferedImage removerTransparencia(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_INT_RGB) {
            return original;
        }

        BufferedImage semTransparencia =
                new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graficos = semTransparencia.createGraphics();
        graficos.setColor(Color.WHITE);
        graficos.fillRect(0, 0, original.getWidth(), original.getHeight());
        graficos.drawImage(original, 0, 0, null);
        graficos.dispose();

        return semTransparencia;
    }
}
