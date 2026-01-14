# API Inventory Template

Use this structure when documenting the JS/TS API before comparing with Java.

```
## JavaScript API

### browser
- launch(options?: LaunchOptions): Promise<Vibe>

### Vibe
- go(url: string): Promise<void>
- screenshot(): Promise<Buffer>
- evaluate<T>(script: string): Promise<T>
- find(selector: string, options?: FindOptions): Promise<Element>
- quit(): Promise<void>

### Element
- click(options?: ActionOptions): Promise<void>
- type(text: string, options?: ActionOptions): Promise<void>
- text(): Promise<string>
- getAttribute(name: string): Promise<string | null>
- boundingBox(): Promise<BoundingBox>
- info: ElementInfo (readonly)

### Types
- LaunchOptions: { headless?: boolean, port?: number, executablePath?: string }
- FindOptions: { timeout?: number }
- ActionOptions: { timeout?: number }
- BoundingBox: { x: number, y: number, width: number, height: number }
- ElementInfo: { tag: string, text: string, box: BoundingBox }

### Errors
- ConnectionError(url: string, cause?: Error)
- TimeoutError(selector: string, timeout: number, reason?: string)
- ElementNotFoundError(selector: string)
- BrowserCrashedError(exitCode: number, output?: string)
```
