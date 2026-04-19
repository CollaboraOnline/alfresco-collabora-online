/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */
import { Injectable, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Node, NodeEntry } from '@alfresco/js-api';
import { Router, NavigationEnd } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AppConfigService } from '@alfresco/adf-core';
import { AlfrescoApiService } from '@alfresco/adf-content-services';
import { BaseCollaboraService } from './base-collabora.service';
import { AdfHttpClient } from '@alfresco/adf-core/api';

@Injectable({
  providedIn: 'root'
})
export class CollaboraOnlineService extends BaseCollaboraService {
  private readonly destroyRef = inject(DestroyRef);
  private previousUrl: string;
  private currentUrl: string;
  isLoading = false;
  displayNode: Node | null = null;

  constructor(
    private router: Router,
    protected adfHttpClient: AdfHttpClient,
    protected apiService: AlfrescoApiService,
    protected appConfig: AppConfigService
  ) {
    super(adfHttpClient);
    this.currentUrl = this.router.url;
    this.router.events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.previousUrl = this.currentUrl;
        this.currentUrl = event.url;
      }
    });
  }

  onEdit(nodeEntry: NodeEntry): void {
    if (nodeEntry?.entry?.isFile) {
      this.triggerEditCollaboraOnline(nodeEntry.entry);
    } else {
      console.warn('Node is not a file or is invalid:', nodeEntry);
    }
  }

  onView(nodeEntry: NodeEntry): void {
    if (nodeEntry?.entry?.isFile) {
      this.triggerViewWithCollaboraOnline(nodeEntry.entry);
    }
  }
  private triggerEditCollaboraOnline(node: Node): void {
    const navigationPath = ['/collabora-online', 'edit', node.id];
    void this.router.navigate(navigationPath);
  }

  private triggerViewWithCollaboraOnline(node: Node): void {
    const navigationPath = ['/collabora-online', 'view', node.id];
    void this.router.navigate(navigationPath);
  }

  async getLoolUrl(): Promise<string> {
    const loolHostUrl: { lool_host_url: string } = await firstValueFrom(this.get('alfresco/s/lool/host/url'));
    return loolHostUrl.lool_host_url;
  }

  async getAccessToken(nodeId: string, action: string): Promise<any> {
    return firstValueFrom(this.get(`/alfresco/s/lool/token?nodeRef=workspace://SpacesStore/${nodeId}&action=${action}`));
  }

  getPreviousUrl(): string {
    return this.previousUrl;
  }

  getExtensions(): string[] {
    const enable = this.appConfig.get<boolean>('collabora.enable');
    if (!enable) {
      return [];
    }
    const extCanView = this.appConfig.get<string[]>('collabora.view') || [];
    const extCanEdit = this.appConfig.get<string[]>('collabora.edit') || [];

    return [...extCanEdit, ...extCanView];
  }
}
