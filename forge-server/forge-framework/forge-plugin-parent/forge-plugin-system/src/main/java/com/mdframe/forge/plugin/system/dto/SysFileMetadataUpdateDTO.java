package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Only the display group may change through the metadata update endpoint. */
@Data
public class SysFileMetadataUpdateDTO {
    @NotNull
    private Long id;
    private Long groupId;
}
