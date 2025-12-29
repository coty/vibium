import { BiDiMessage } from './types';
import { ConnectionError } from '../utils/errors';

// Use native WebSocket on Bun, ws package on Node.js
declare const Bun: unknown;
const isBun = typeof Bun !== 'undefined';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyWebSocket = any;

export type MessageHandler = (msg: BiDiMessage) => void;

export class BiDiConnection {
  private ws: AnyWebSocket;
  private messageHandler: MessageHandler | null = null;
  private closePromise: Promise<void>;
  private _closed: boolean = false;

  private constructor(ws: AnyWebSocket, useBrowserApi: boolean) {
    this.ws = ws;

    if (useBrowserApi) {
      // Browser/Bun WebSocket API
      this.closePromise = new Promise((resolve) => {
        ws.onclose = () => {
          this._closed = true;
          resolve();
        };
      });

      ws.onmessage = (event: MessageEvent) => {
        if (this.messageHandler) {
          try {
            const msg = JSON.parse(event.data) as BiDiMessage;
            this.messageHandler(msg);
          } catch (err) {
            console.error('Failed to parse BiDi message:', err);
          }
        }
      };
    } else {
      // Node.js ws package API
      this.closePromise = new Promise((resolve) => {
        ws.on('close', () => {
          this._closed = true;
          resolve();
        });
      });

      ws.on('message', (data: Buffer | string) => {
        if (this.messageHandler) {
          try {
            const msg = JSON.parse(data.toString()) as BiDiMessage;
            this.messageHandler(msg);
          } catch (err) {
            console.error('Failed to parse BiDi message:', err);
          }
        }
      });
    }
  }

  static async connect(url: string): Promise<BiDiConnection> {
    if (isBun) {
      // Use native WebSocket on Bun
      return new Promise((resolve, reject) => {
        const ws = new WebSocket(url);

        ws.onopen = () => {
          resolve(new BiDiConnection(ws, true));
        };

        ws.onerror = (err: Event) => {
          reject(new ConnectionError(url, new Error(String(err))));
        };
      });
    } else {
      // Use ws package on Node.js
      const { default: WS } = await import('ws');
      return new Promise((resolve, reject) => {
        const ws = new WS(url);

        ws.on('open', () => {
          resolve(new BiDiConnection(ws, false));
        });

        ws.on('error', (err: Error) => {
          reject(new ConnectionError(url, err));
        });
      });
    }
  }

  get closed(): boolean {
    return this._closed;
  }

  onMessage(handler: MessageHandler): void {
    this.messageHandler = handler;
  }

  send(message: string): void {
    if (this._closed) {
      throw new Error('Connection closed');
    }
    this.ws.send(message);
  }

  async close(): Promise<void> {
    if (this._closed) {
      return;
    }
    this.ws.close();
    await this.closePromise;
  }
}
