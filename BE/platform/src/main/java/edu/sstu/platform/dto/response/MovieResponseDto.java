package edu.sstu.platform.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class MovieResponseDto {

  private UUID id;
  private String engTitle;
  private String rusTitle;
  private int year;
  private double totalRating;
}
