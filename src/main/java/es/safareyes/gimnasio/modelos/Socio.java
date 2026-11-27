package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "socio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Socio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_socio")
    private Long id;

    @Column(name = "dni", nullable = false, unique = true, length = 15)
    private String dni;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "telf", length = 20)
    private String telefono;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "matricula", length = 7)
    private String matricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan")
    private Plan plan;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
    }
}
