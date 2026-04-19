/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { CollaboraOnlineService } from './collabora-online.service';
import { AppConfigService } from '@alfresco/adf-core';
import { Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

describe('CollaboraOnlineService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CollaboraOnlineService,
        AppConfigService,
        {
          provide: Router,
          useValue: { url: '/test', events: { pipe: () => ({ subscribe: () => {} }) } }
        }
      ]
    });
  });

  it('should be created', () => {
    const service = TestBed.inject(CollaboraOnlineService);
    expect(service).toBeTruthy();
  });
});
