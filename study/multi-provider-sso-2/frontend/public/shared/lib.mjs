// @ts-nocheck
/**
 * Shared Preact + HTM library bridge.
 * Used by: Architectures 1, 2, 3, 4, and Portal.
 */

// import { h, render } from 'preact'
// import { useState, useEffect } from 'preact/hooks'
// import htm from 'htm/preact'
import { html, render, useState, useEffect } from 'https://unpkg.com/htm@3/preact/standalone.module.js';

// const html = htm.bind(h)

// export { h, render, useState, useEffect, html }
export { html, render, useState, useEffect }
