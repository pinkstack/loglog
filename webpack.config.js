const path = require('path');
const loader = require('sass-loader');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

const baseEntries = [
  __dirname + '/frontend/src/main/scss/styles.scss'
]

const productionEntries = baseEntries.concat([
  __dirname + '/frontend/target/scala-3.1.1/frontend-opt/main.js',
]);

const developmentEntries = baseEntries.concat([
  __dirname + '/frontend/target/scala-3.1.1/frontend-fastopt/main.js',
]);

var config = {
  mode: 'development',
  entry: developmentEntries,

  plugins: [
    new MiniCssExtractPlugin({
      filename: 'style.css'
    }),
    new CleanWebpackPlugin({
      verbose: true
    }),
    new HtmlWebpackPlugin({
      inject: true,
      title: 'loglog for RTV 356',
      publicPath: '/',
      template: './frontend/src/main/html/index.html'
    })
  ],

  output: {
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'dist'),
  },

  devServer: {
    static: './dist',
    hot: true
  },

  optimization: {
    runtimeChunk: 'single',
    splitChunks: { chunks: 'all'},
  },

  module: {
    rules: [
      {
        test: new RegExp("\\.js$"),
        enforce: "pre",
        use: ["source-map-loader"]
      }, 
      {
        enforce: "pre",
        test: /\.js$/,
        loader: "source-map-loader",
        exclude: [/node_modules/, /build/, /__test__/, /__zio__/]
      },
      {
        test: /\.s[ac]ss$/i,
        use: [
          MiniCssExtractPlugin.loader,
          "css-loader",
          {
            loader: "sass-loader",
            options: {
              sourceMap: true,
              sassOptions: { outputStyle: "compressed" },
            },
          }],
      }
    ]
  },

  ignoreWarnings: [/Failed to parse source map/],
};

module.exports = (env, argv) => {
  if (argv.mode === 'development') {
    config.devtool = 'source-map';
  }

  if (argv.mode === 'production') {
    config.entry = productionEntries;
    config.output.filename = '[name].[contenthash].js';
  }

  return config;
};
