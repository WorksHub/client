/* global module, require, __dirname */

const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');

module.exports = {
  entry: ['./client/src-js/index.js', './client/styles/wh.sass'],
  output: {
    path: path.resolve(__dirname, 'client/resources/public'),
    filename: 'public.js'
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: './wh.css'
    })
  ],
  mode: 'development',
  devtool: 'source-map',
  module: {
    rules: [
      {
        test: /\.sass$/,
        use: [
          {
            loader: MiniCssExtractPlugin.loader
          },
          {
            loader: 'css-loader?-url'
          },
          {
            loader: 'postcss-loader'
          },
          {
            loader: 'sass-loader'
          }
        ]
      }
    ]
  }
};
