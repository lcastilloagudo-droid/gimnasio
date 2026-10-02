package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "monitor")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Monitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_monitor")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellidos")
    private String apellidos;
}
