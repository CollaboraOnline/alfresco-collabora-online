<!--
Copyright (C) 2022 - Jeci SARL - https://jeci.fr
SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr

SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div id="loolcontainer">
    <form
      id="loleafletform"
      name="loleafletform"
      method="POST"
      :action="`${collaboraUrl}WOPISrc=${encodeURIComponent(wopiFileUrl)}&lang=${lang}`"
      target="loleafletframe"
      hidden="hidden"
    >
      <input type="hidden" name="access_token" :value="accessToken" />
      <input type="hidden" name="access_token_ttl" :value="accessTokenTTL" />
      <input type="hidden" name="ui_defaults" :value="uiDefaults" />
      <input type="hidden" name="css_variables" :value="cssVariables" />
    </form>
    <iframe
      id="loleafletframe"
      name="loleafletframe"
      :allow="`clipboard-read *; clipboard-write *; fullscreen 'self' ${collaboraOrigin};`"
    />
  </div>
</template>
<script>
export default {
  name: "CollaboraIframe",
  emits: ["doc-modified", "app-loading-status"],
  props: {
    accessToken: {
      type: String,
      default: null,
    },
    accessTokenTTL: {
      type: String,
      default: null,
    },
    wopiFileUrl: {
      type: String,
      default: null,
    },
    collaboraUrl: {
      type: String,
      default: "",
    },
    lang: {
      type: String,
      default: "en",
    },
    uiMode: {
      type: String,
      default: "tabbed",
    },
    cssVariables: {
      type: String,
      default: "",
      description:
        "https://sdk.collaboraonline.com/docs/theming.html#available-variables",
    },
  },
  data() {
    return {
      loading: {
        wopi: true,
        collab: true,
      },
    };
  },
  computed: {
    collaboraOrigin() {
      if (!this.collaboraUrl) {
        return "";
      }
      try {
        const url = new URL(this.collaboraUrl);
        return url.origin;
      } catch {
        return "";
      }
    },
    uiDefaults() {
      // https://sdk.collaboraonline.com/docs/theming.html
      return `UIMode=${this.uiMode}`;
    },
  },
  watch: {
    wopiFileUrl() {
      this.loading.wopi = false;
    },
    collaboraUrl() {
      this.loading.collab = false;
    },
    //Loading is watched when the component is imported in another project
    loading: {
      handler() {
        if (this.collaboraUrl && this.wopiFileUrl) {
          this.$nextTick(() => {
            this.$el.children.loleafletform.requestSubmit();
          });
        }
      },
      deep: true,
    },
  },
  //Mounted is used when the component is imported in the current project
  mounted() {
    window.addEventListener("message", this.handlePostMessage);
    if (this.collaboraUrl && this.wopiFileUrl) {
      this.$nextTick(() => {
        this.$el.children.loleafletform.requestSubmit();
      });
    }
  },
  unmounted() {
    window.removeEventListener("message", this.handlePostMessage);
  },
  methods: {
    /**
     * Dispatch incoming postMessage events from Collabora Online.
     * Validates the origin against the Collabora server URL before processing.
     * @see https://sdk.collaboraonline.com/docs/postmessage_api.html
     */
    handlePostMessage(event) {
      if (!this.collaboraOrigin || event.origin !== this.collaboraOrigin) {
        return;
      }

      let msg;
      try {
        msg =
          typeof event.data === "string" ? JSON.parse(event.data) : event.data;
      } catch {
        return;
      }

      if (!msg || !msg.MessageId) {
        return;
      }

      switch (msg.MessageId) {
        case "Doc_ModifiedStatus":
          this.$emit("doc-modified", msg.Values);
          break;
        case "App_LoadingStatus":
          this.$emit("app-loading-status", msg.Values);
          break;
      }
    },
    /**
     * Send a postMessage to the Collabora Online iframe.
     * @param {string} messageId - The MessageId to send
     * @param {object} values - The Values payload
     */
    sendMessage(messageId, values = {}) {
      const iframe = this.$el.querySelector("#loleafletframe");
      if (!iframe?.contentWindow || !this.collaboraOrigin) {
        return;
      }
      iframe.contentWindow.postMessage(
        JSON.stringify({
          MessageId: messageId,
          SendTime: Date.now(),
          Values: values,
        }),
        this.collaboraOrigin,
      );
    },
  },
};
</script>
<style lang="scss" scoped>
#loleafletframe {
  width: 100%;
  height: 100%;
  border: 0 none;
}

#loolcontainer {
  width: 100%;
  height: 100%;
  background: none;
}
</style>
