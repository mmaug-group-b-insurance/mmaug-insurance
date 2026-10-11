package com.mmaug.claimsagent.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Statement edit submitted by an officer during review")
public class StatementEdit {

    @NotNull
    private UUID statementId;

    @NotBlank
    @Size(max = 1500)
    private String editedText;

    @NotBlank
    @Size(max = 500)
    private String reason;
}