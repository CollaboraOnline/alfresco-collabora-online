/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */
import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { UserPreferencesService, ThumbnailService } from '@alfresco/adf-core';
import { CollaboraOnlineComponent } from './collabora-online.component';
import { CollaboraOnlineService } from '../services/collabora-online.service';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, ContentApiService } from '@alfresco/aca-shared';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { TranslateService, TranslateStore } from '@ngx-translate/core';

describe('CollaboraOnlineComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: CollaboraOnlineService,
          useValue: {
            getLoolUrl: jasmine.createSpy('getLoolUrl').and.returnValue(Promise.resolve('http://localhost:9980/')),
            getAccessToken: jasmine
              .createSpy('getAccessToken')
              .and.returnValue(Promise.resolve({ access_token: 'token', access_token_ttl: '3600', wopi_src_url: 'http://localhost:9980/' })),
            getPreviousUrl: jasmine.createSpy('getPreviousUrl').and.returnValue('/')
          }
        },
        {
          provide: UserPreferencesService,
          useValue: { select: jasmine.createSpy('select').and.returnValue(of('en')) }
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { nodeId: 'test-node-id', action: 'edit' } } }
        },
        {
          provide: ContentApiService,
          useValue: {
            getNode: jasmine.createSpy('getNode').and.returnValue(
              of({
                entry: {
                  name: 'test.docx',
                  content: { mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' }
                }
              })
            )
          }
        },
        {
          provide: Router,
          useValue: { navigateByUrl: jasmine.createSpy('navigateByUrl'), url: '/test' }
        },
        {
          provide: ThumbnailService,
          useValue: { getMimeTypeIcon: jasmine.createSpy('getMimeTypeIcon').and.returnValue('adf:file-word') }
        },
        {
          provide: AppService,
          useValue: { setAppNavbarMode: jasmine.createSpy('setAppNavbarMode') }
        },
        {
          provide: TranslateService,
          useValue: {
            instant: jasmine.createSpy('instant').and.callFake((key: string) => key),
            get: jasmine.createSpy('get').and.returnValue(of('')),
            stream: jasmine.createSpy('stream').and.returnValue(of('')),
            onLangChange: of(),
            onTranslationChange: of(),
            onDefaultLangChange: of()
          }
        },
        {
          provide: TranslateStore,
          useValue: {}
        }
      ]
    });
  });

  it('should be created', () => {
    const fixture = TestBed.createComponent(CollaboraOnlineComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
