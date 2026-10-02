package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_plan")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo")
    @Enumerated(EnumType.ORDINAL)
    private Tipo tipo;
}
