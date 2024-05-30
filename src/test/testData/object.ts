import { defineSuite } from "esbench";

declare function welchTest(a: number[], b: number[], h: string): number;

const a = [19.8, 20.4, 19.6, 17.8, 18.5, 18.9];
const b = [28.2, 26.6, 20.1, 23.3, 25.2];

export default <caret>defineSuite({
    setup(scene){
        scene.bench("welch", () => welchTest(a, b, "not equal"));
    }
});
