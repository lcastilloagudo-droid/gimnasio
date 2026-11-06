package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_socio")
    private Socio idSocio;

    @Column(name = "nombre_usuario", nullable = false, unique = true)
    private String nomUsuario;

    @Column(name = "contrasena", nullable = false)
    private String contrasenya;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;


}
