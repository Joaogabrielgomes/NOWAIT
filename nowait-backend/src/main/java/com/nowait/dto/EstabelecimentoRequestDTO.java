package com.nowait.dto;

import com.nowait.model.CategoriaEstabelecimento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class EstabelecimentoRequestDTO {

    @NotBlank(message = "Nome do estabelecimento é obrigatório")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotBlank(message = "Categoria é obrigatória")
    @Pattern(regexp = CategoriaEstabelecimento.TODAS, message = "Categoria inválida")
    private String categoria;

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}$", message = "Telefone inválido")
    private String telefone;

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido")
    private String cep;

    @NotBlank(message = "Número é obrigatório")
    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
    private String numero;

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complemento;

    @Min(value = 5, message = "Tolerância mínima é de 5 minutos")
    @Max(value = 120, message = "Tolerância máxima é de 120 minutos")
    private Integer toleranciaPadraoMinutos;

    @Min(value = 5, message = "Tempo médio de atendimento mínimo é de 5 minutos")
    @Max(value = 240, message = "Tempo médio de atendimento máximo é de 240 minutos")
    private Integer tempoMedioAtendimentoMinutos;

    private LocalTime horarioAbertura;

    private LocalTime horarioFechamento;

    @Min(value = 1, message = "Antecedência máxima mínima é de 1 dia")
    @Max(value = 90, message = "Antecedência máxima é de 90 dias")
    private Integer antecedenciaMaximaDias;

    @Min(value = 0, message = "Antecedência de reserva de mesa não pode ser negativa")
    @Max(value = 120, message = "Antecedência de reserva de mesa máxima é de 120 minutos")
    private Integer antecedenciaReservaMesaMinutos;

    @Min(value = 0, message = "Antecedência mínima não pode ser negativa")
    @Max(value = 1440, message = "Antecedência mínima máxima é de 1440 minutos (24h)")
    private Integer antecedenciaMinimaMinutos;
}
