package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "actividad")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_actividad")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_monitor", nullable = false)
    private Monitor idMonitor;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "duracion")
    private LocalDateTime duracion;

    @Column(name = "sala")
    private String sala;

    @Column(name = "aforo_max")
    private Integer aforoMax;
}
