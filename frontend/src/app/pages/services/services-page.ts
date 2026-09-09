import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MedicalServiceApi } from '../../services/medical-service.service';
import { MedicalServiceDTO } from '../../entities/medical-service';
import { imageUrl } from '../../util/media';

@Component({
  selector: 'app-services-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './services-page.html',
  styleUrl: './services-page.css',
})
export class ServicesPage implements OnInit {
  private readonly api = inject(MedicalServiceApi);

  readonly services = signal<MedicalServiceDTO[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly imageUrl = imageUrl;

  ngOnInit(): void {
    this.api.listActive().subscribe({
      next: (services) => {
        this.services.set(services);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Nu am putut încărca lista de servicii. Încearcă din nou mai târziu.');
        this.loading.set(false);
      },
    });
  }
}
