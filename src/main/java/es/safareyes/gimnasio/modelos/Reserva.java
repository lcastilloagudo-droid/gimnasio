package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_reserva")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_socio", nullable = false)
    private Socio idSocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad", nullable = false)
    private Actividad idActividad;

    @Column(name = "fecha_reserva")
    private LocalDateTime fechaReserva;

    @Column(name = "estado")
    @Enumerated(EnumType.ORDINAL)
    private Estado estado;
}
