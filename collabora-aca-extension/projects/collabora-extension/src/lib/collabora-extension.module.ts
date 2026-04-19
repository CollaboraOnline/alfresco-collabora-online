/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */

import { provideTranslations } from '@alfresco/adf-core';
import { provideExtensionConfig, provideExtensions } from '@alfresco/adf-extensions';
import { EnvironmentProviders, Provider } from '@angular/core';
import * as rules from './rules/evaluators';
import { provideEffects } from '@ngrx/effects';
import { CollaboraEffects } from './store/effects/collabora-online.effects';
import { CollaboraOnlineComponent } from './components/collabora-online.component';

export function provideCollaboraExtension(): (Provider | EnvironmentProviders)[] {
  return [
    provideTranslations('collabora-extension', 'assets/collabora-extension'),
    provideEffects([CollaboraEffects]),
    provideExtensionConfig(['collabora-extension.json']),
    provideExtensions({
      components: {
        'collabora.edit.component': CollaboraOnlineComponent,
        'collabora.view.component': CollaboraOnlineComponent
      },
      evaluators: {
        'collabora.canEditWithCollaboraOnline': rules.canEditWithCollaboraOnline,
        'collabora.canViewWithCollaboraOnline': rules.canViewWithCollaboraOnline
      }
    })
  ];
}
