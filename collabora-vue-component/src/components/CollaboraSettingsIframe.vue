<!--
SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr

SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div id="settingscontainer">
    <form
      id="settingsform"
      name="settingsform"
      method="POST"
      :action="formAction"
      target="settingsframe"
      hidden="hidden"
    >
      <input type="hidden" name="access_token" :value="accessToken" />
      <input
        type="hidden"
        name="wopi_setting_base_url"
        :value="wopiSettingBaseUrl"
      />
      <input type="hidden" name="iframe_type" :value="iframeType" />
      <input v-if="uiTheme" type="hidden" name="ui_theme" :value="uiTheme" />
      <input
        v-if="cssVariables"
        type="hidden"
        name="css_variables"
        :value="cssVariables"
      />
    </form>
    <iframe id="settingsframe" name="settingsframe" :style="iframeStyle" />
  </div>
</template>
<script>
/**
 * Renders the Collabora Online settings management iframe (WOPI Settings API).
 * @see https://sdk.collaboraonline.com/docs/advanced_integration.html
 *
 * The {@code settingsUrl} is the {@code urlsrc} of the "Settings" action from the Collabora discovery.xml. Depending
 * on the server it may or may not already end with {@code ?}, so the {@code lang} query parameter is appended with the
 * right separator. Collabora derives the {@code /wopi/settings} endpoint it calls from {@code wopiSettingBaseUrl}.
 */
export default {
  name: "CollaboraSettingsIframe",
  emits: ["iframe-height"],
  props: {
    accessToken: {
      type: String,
      default: null,
    },
    settingsUrl: {
      type: String,
      default: "",
      description:
        "urlsrc of the 'settings' action from the Collabora discovery.xml",
    },
    wopiSettingBaseUrl: {
      type: String,
      default: "",
      description:
        "WOPI base URL Collabora appends '/settings' to (e.g. https://acs/alfresco/s/wopi)",
    },
    iframeType: {
      type: String,
      default: "user",
      validator: (value) => ["admin", "user"].includes(value),
    },
    lang: {
      type: String,
      default: "en",
    },
    uiTheme: {
      type: String,
      default: "",
      validator: (value) => ["", "light", "dark"].includes(value),
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
      iframeHeight: null,
    };
  },
  computed: {
    settingsOrigin() {
      if (!this.settingsUrl) {
        return "";
      }
      try {
        const url = new URL(this.settingsUrl);
        return url.origin;
      } catch {
        return "";
      }
    },
    formAction() {
      // The discovery urlsrc may end with "?", contain a query string, or end with neither.
      // Append lang with the matching separator so we never produce ".htmllang=fr".
      const url = this.settingsUrl || "";
      if (!url) {
        return url;
      }
      let separator = "?";
      if (url.includes("?")) {
        separator = url.endsWith("?") || url.endsWith("&") ? "" : "&";
      }
      return `${url}${separator}lang=${this.lang}`;
    },
    iframeStyle() {
      // Collabora drives the height via the Iframe_Height postMessage; fall back to full height before the first one.
      return this.iframeHeight
        ? { width: "100%", height: `${this.iframeHeight}px` }
        : { width: "100%", height: "100%" };
    },
  },
  watch: {
    settingsUrl() {
      this.submitForm();
    },
    accessToken() {
      this.submitForm();
    },
  },
  mounted() {
    window.addEventListener("message", this.handlePostMessage);
    this.submitForm();
  },
  unmounted() {
    window.removeEventListener("message", this.handlePostMessage);
  },
  methods: {
    submitForm() {
      if (this.settingsUrl && this.accessToken) {
        this.$nextTick(() => {
          this.$el.children.settingsform.requestSubmit();
        });
      }
    },
    /**
     * Dispatch incoming postMessage events from the Collabora settings iframe.
     * Validates the origin against the Collabora server URL before processing.
     * @see https://sdk.collaboraonline.com/docs/postmessage_api.html
     */
    handlePostMessage(event) {
      if (!this.settingsOrigin || event.origin !== this.settingsOrigin) {
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

      if (msg.MessageId === "Iframe_Height") {
        const height = Number.parseInt(msg.Values?.height ?? msg.Values, 10);
        if (!Number.isNaN(height)) {
          this.iframeHeight = height;
        }
        this.$emit("iframe-height", msg.Values);
      }
    },
  },
};
</script>
<style lang="scss" scoped>
#settingsframe {
  width: 100%;
  height: 100%;
  border: 0 none;
}

#settingscontainer {
  width: 100%;
  height: 100%;
  background: none;
}
</style>
