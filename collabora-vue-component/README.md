# Collabora in a Vue 3 + Vite component

This project is two-fold in how to display the Collabora Online iframe :
- a Vue3 component that can be used in any vue3 application
- an independent application that can run anywhere

It is prepared and build with vite. It also can be run with it.

Usual commands :
- `npm run build` to build the library that can be imported anywhere
- `npm run dev` to show the application

The example.html is there for dev purposes, change AppExample.vue to your fit.
Navigate to http://localhost:8008/example.html

The main view (http://localhost:8008/index.html) is the independent application. Just give the right paramaters and it should show you Collabora :
- accessToken : the wopi token generated by the backend
- accessTokenTTL : and its TTL
- collaboraUrl : the cool.html url
- wopiFileUrl : the wopi URL of the file you want to access with Collabora (has to be understood by the Collabora Server)

Example :
http://localhost:8008/index.html?collaboraUrl=http://localhost:9980/browser/b639546/cool.html?&wopiFileUrl=http://acs:8080/alfresco/service/wopi/files/0dde2d84-0ba6-4f6e-9f0a-0eb9f15d9888&access_token=ghseipu77bseoo1jgodbj7ci75&access_token_ttl=1666803915100

Changelog

* v0.1.0 : inital public release
* v0.1.1 : update dependencies (axios, saas, vite)
* v0.2.0 : add lang, ui_defaults and css_variables parameters