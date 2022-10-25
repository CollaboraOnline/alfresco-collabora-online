<!--
  Copyright (C) 2022 - Jeci SARL - https://jeci.fr

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see https://www.gnu.org/licenses/agpl-3.0.html.
-->

<!--
// Copyright (c) 2018 Ross Kaffenberger
// Copyright (c) 2022 Jérémie Lesage - Jeci
// -->
<template>
  <h1>test collabora</h1>
  <div id="loolcontainer">
    <input type="submit" @click="postForm()" />
    <form
      id="loleafletform"
      ref="collaboraForm"
      name="loleafletform"
      method="POST"
      :action="`${collaboraUrl}WOPISrc=${wopiFileUrl}`"
      target="loleafletframe"
      hidden="hidden"
    >
      <input type="text" name="access_token" :value="accessToken" />
      <input type="text" name="access_token_ttl" :value="accessTokenTTL" />
    </form>
    <iframe
      id="loleafletframe"
      name="loleafletframe"
      allow="fullscreen"
    ></iframe>
  </div>
</template>
<script>
export default {
  name: "CollaboraIframe",
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
  },
  data() {
    return {
      listenerHandlePostMessage: null,
      iFrameUrl: {
        type: String,
        default: null,
      },
      loading: true,
    };
  },
  watch: {
    loading(newLoadingValue) {
      console.log("loading", newLoadingValue, this.wopiFileUrl);
      if (this.wopiFileUrl !== null) {
        this.$refs.collaboraForm.requestSubmit();
      }
    },
  },
  mounted() {
    this.loading = false;
  },
  unmounted() {
    window.removeEventListener("message", this.listenerHandlePostMessage, true);
  },
  methods: {
    handlePostMessage(event) {
      console.log("handlePostMessage event data:");
      console.log(event.data);
      let message = JSON.parse(event.data);
      let id = message.MessageId;
      let values = message.Values;

      switch (id) {
        case "UI_Close":
          console.log("PostMessage Recev: UI_CLose - move to");
          // Go back to previous page
          //this.router.navigateByUrl(this.previousUrl);
          break;
        case "App_LoadingStatus":
          if (values.Status === "Frame_Ready") {
            // Add readonly
          }
          console.log(
            "PostMessage Recev: App_LoadingStatus - Status:" + values.Status
          );
          break;
        case "View_Added":
          console.log(
            "PostMessage Recev: View_Added - Values:" + JSON.stringify(values)
          );
          break;
        default:
          console.log("MessageID : " + id + " - Values : " + values);
      }
    },
    postForm() {
      this.$refs.collaboraForm.submit();
    },
  },
};
</script>
<style lang="scss">
#loleafletframe {
  width: 100%;
  height: 100%;
  border: 0 none;
}

#loolcontainer {
  bottom: 0;
  width: 100%;
  top: 0;
  background: none;
}
</style>
