import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.HibernateTaskRepository;
import org.prongpa.Repository.Task.TaskRepository;

import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskRepositoryTest {
    private SessionFactory sessionFactory;
    private TaskRepository taskRepository;

    @BeforeEach
    public void setup() {
        // Configurar la sesión de Hibernate utilizando el archivo de configuración
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        sessionFactory = configuration.buildSessionFactory();
        // Crear una instancia del repositorio HibernateTaskRepository
        taskRepository = new HibernateTaskRepository(sessionFactory);
    }

    @Test
    @Order(1)
    public void testSave() {
        // Crear una nueva tarea
        TaskModel task = new TaskModel();
        task.setEstado("I");
        task.setLastDate(new Date());
        task.setProcess_id("1");
        task.setThread_id(1);
        task.setModel("modelo");
        task.setReintentos(1);
        task.setVersion("1.0");
        task.setTasktype("tipo");
        task.setIdcompuesto("compuesto");
        task.setFilename("archivo.txt");
        task.setManufacturer("fabricante");
        task.setNotificationId("notification-123");
        task.setCommands("comandos");
        task.setOrderTask(4);
        // Establecer otros atributos

        // Guardar la tarea utilizando el repositorio
        assertEquals(true,taskRepository.save(task));
    }

    // Agregar más pruebas para otros métodos y escenarios

    // ...
    @Test
    @Order(2)
    public void testFindByEstado() {
        // Realizar una consulta utilizando el repositorio
        List<TaskModel> tasks = taskRepository.findByEstado("I");

        // Verificar si hay elementos en la lista
        assertTrue(!tasks.isEmpty());

        // ...comparar otros atributos de las tareas con los valores esperados
    }
    @Test
    @Order(3)
    public void updatebyprocessid() {
        // Realizar una consulta utilizando el repositorio
        String processId = "12345";

        // Actualizar el estado de las tareas con el processId dado
        boolean updateResult = taskRepository.UpdateByProcessId(processId);

        // Verificar los resultados esperados
        assertTrue(updateResult);

        // ...comparar otros atributos de las tareas con los valores esperados
    }
    @Test
    @Order(4)
    public void deletebyprocessid() {
        // Realizar una consulta utilizando el repositorio
        String processId = "12345";

        // Eliminar las tareas con el processId dado
        boolean deleteResult = taskRepository.deleteByProcessId(processId);

        // Verificar los resultados esperados
        assertTrue(deleteResult);

        // ...comparar otros atributos de las tareas con los valores esperados
    }
    @AfterEach
    public void cleanUp() {
        sessionFactory.close();
    }
}
