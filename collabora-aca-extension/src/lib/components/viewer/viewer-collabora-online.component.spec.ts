/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ViewerCollaboraOnlineComponent } from './viewer-collabora-online.component';
import { provideHttpClient } from '@angular/common/http';
import { UserPreferencesService } from '@alfresco/adf-core';
import { of } from 'rxjs';
import { CollaboraOnlineService } from '../../services/collabora-online.service';

describe('CollaboraComponent', () => {
  let component: ViewerCollaboraOnlineComponent;
  let fixture: ComponentFixture<ViewerCollaboraOnlineComponent>;

  beforeEach(waitForAsync(() => {
    void TestBed.configureTestingModule({
      imports: [ViewerCollaboraOnlineComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: UserPreferencesService,
          useValue: { select: jasmine.createSpy('select').and.returnValue(of('en')) }
        },
        {
          provide: CollaboraOnlineService,
          useValue: {
            getLoolUrl: jasmine.createSpy('getLoolUrl').and.returnValue(Promise.resolve('http://localhost:9980/')),
            getAccessToken: jasmine
              .createSpy('getAccessToken')
              .and.returnValue(Promise.resolve({ access_token: 'token', access_token_ttl: '3600', wopi_src_url: 'http://localhost:9980/' }))
          }
        }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ViewerCollaboraOnlineComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
