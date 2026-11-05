package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateNodeRequest(
        @NotBlank(message = "label must not be blank")
        String label,

        String properties,

        List<@NotBlank(message = "tag must not be blank")String> tags
) {

    public CreateNodeRequest{
        label = label == null ? null : label.trim();
        properties = properties == null? "":properties;
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

}
