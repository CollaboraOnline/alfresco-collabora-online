/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, OnInit, OnDestroy, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentApiService, AppService } from '@alfresco/aca-shared';
import { NodeEntry } from '@alfresco/js-api';
import {
  UserPreferencesService,
  ToolbarComponent,
  ToolbarTitleComponent,
  ToolbarDividerComponent,
  ViewerToolbarActionsComponent,
  ThumbnailService
} from '@alfresco/adf-core';
import { CollaboraOnlineService } from '../services/collabora-online.service';
import { firstValueFrom } from 'rxjs';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { CollaboraBaseComponent } from './base/collabora-base.component';

@Component({
  selector: 'collabora-online',
  templateUrl: './collabora-online.component.html',
  styleUrls: ['./collabora-online.component.scss'],
  encapsulation: ViewEncapsulation.None,
  imports: [ToolbarComponent, ToolbarTitleComponent, MatIcon, ToolbarDividerComponent, ViewerToolbarActionsComponent, TranslatePipe]
})
export class CollaboraOnlineComponent extends CollaboraBaseComponent implements OnInit, OnDestroy {
  nodeEntry: NodeEntry | null = null;
  fileName: string;
  mimeType: string;
  mimeTypeIcon: string;
  listenerHandlePostMessage: ((event: MessageEvent) => void) | null = null;

  constructor(
    protected route: ActivatedRoute,
    protected collaboraOnlineService: CollaboraOnlineService,
    protected userPreferencesService: UserPreferencesService,
    private contentApi: ContentApiService,
    private router: Router,
    private thumbnailService: ThumbnailService,
    private appService: AppService
  ) {
    super(route, collaboraOnlineService, userPreferencesService);
  }

  async ngOnInit(): Promise<void> {
    try {
      this.nodeEntry = await firstValueFrom(this.contentApi.getNode(this.nodeId));
      if (this.nodeEntry) {
        this.fileName = this.nodeEntry.entry.name;
        if (this.nodeEntry.entry.content) {
          this.mimeType = this.nodeEntry.entry.content.mimeType;
          this.mimeTypeIcon = this.thumbnailService.getMimeTypeIcon(this.mimeType);
        }
      }

      void super.ngOnInit();

      // Add event listener
      if (!this.listenerHandlePostMessage) {
        this.listenerHandlePostMessage = this.handlePostMessage.bind(this);
      }
      window.addEventListener('message', this.listenerHandlePostMessage, true);
    } catch (error) {
      console.error('Error initializing Collabora Online:', error);
      // Navigate back on error
      void this.router.navigateByUrl(this.previousUrl || '/');
    }
  }

  ngOnDestroy(): void {
    // Restore navbar when leaving editor
    this.appService.setAppNavbarMode('expanded');

    if (this.listenerHandlePostMessage) {
      window.removeEventListener('message', this.listenerHandlePostMessage, true);
    }
  }

  private handlePostMessage(event: MessageEvent): void {
    try {
      // Skip if data is not a string or is already an object
      if (typeof event.data !== 'string') {
        return;
      }

      // Try to parse JSON, skip if invalid
      let message;
      try {
        message = JSON.parse(event.data);
      } catch {
        // Not a JSON message, ignore
        return;
      }

      const id = message.MessageId;
      const values = message.Values;

      if (!id) {
        return; // No MessageId, not a Collabora message
      }

      switch (id) {
        case 'UI_Close':
          void this.router.navigateByUrl(this.previousUrl);
          break;
        case 'App_LoadingStatus':
          if (values?.Status === 'Frame_Ready') {
            // Add readonly
          }
          break;
        case 'View_Added':
          break;
        default:
          break;
      }
    } catch (error) {
      console.error('Error handling post message:', error);
    }
  }

  onBackButtonClick(): void {
    // Restore navbar before navigating
    this.appService.setAppNavbarMode('expanded');
    void this.router.navigateByUrl(this.previousUrl);
  }

  onFullscreenClick(): void {
    const container = <any>document.documentElement.querySelector('.adf-viewer__fullscreen-container');
    if (container) {
      if (container.requestFullscreen) {
        container.requestFullscreen();
      } else if (container.webkitRequestFullscreen) {
        container.webkitRequestFullscreen();
      } else if (container.mozRequestFullScreen) {
        container.mozRequestFullScreen();
      } else if (container.msRequestFullscreen) {
        container.msRequestFullscreen();
      }
    }
  }
}
