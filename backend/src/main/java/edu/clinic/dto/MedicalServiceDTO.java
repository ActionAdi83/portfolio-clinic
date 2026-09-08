package edu.clinic.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Request/response body for both the public catalogue and the admin CRUD. */
@Getter
@Setter
public class MedicalServiceDTO {
    private String id;

    @NotBlank
    private String title;

    private String description;
    private String imageUrl;

    @NotNull
    @Min(5)
    private Integer durationMinutes;

    private boolean active = true;
}
