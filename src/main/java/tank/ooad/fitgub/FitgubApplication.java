package tank.ooad.fitgub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

@SpringBootApplication
@EnableAsync
public class FitgubApplication {

	public static void main(String[] args) {
//		var app = new SpringApplication();
//		app.addPrimarySources(List.of(FitgubApplication.class));
//		app.run(args);

		SpringApplication.run(FitgubApplication.class, args);
	}

}
