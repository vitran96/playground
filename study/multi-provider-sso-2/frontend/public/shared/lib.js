/**
 * Shared Preact + HTM library bridge.
 * Used by: Architectures 1, 2, 3, 4, and Portal.
 */

import { h, render } from 'preact'
import { useState, useEffect } from 'preact/hooks'
import htm from 'htm/preact'

const html = htm.bind(h)

export { h, render, useState, useEffect, html }
