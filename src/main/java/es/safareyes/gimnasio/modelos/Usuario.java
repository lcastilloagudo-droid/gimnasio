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
    @JoinColumn(name = "id_socio", unique = true)
    private Socio idSocio;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 100)
    private String nomUsuario;

    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasenya;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private Rol rol;


}
