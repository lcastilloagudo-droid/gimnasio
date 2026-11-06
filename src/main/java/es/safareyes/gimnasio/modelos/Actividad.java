package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_monitor", nullable = false)
    private Monitor idMonitor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "duracion", nullable = false)
    private Integer duracion;

    @Column(name = "sala", nullable = false)
    private String sala;

    @Column(name = "aforo_max", nullable = false)
    private Integer aforoMax;


}
